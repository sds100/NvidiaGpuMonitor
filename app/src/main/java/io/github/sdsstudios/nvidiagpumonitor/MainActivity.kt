package io.github.sdsstudios.nvidiagpumonitor

import android.arch.lifecycle.Observer
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatTextView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.sonelli.juicessh.pluginlibrary.listeners.OnClientStartedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import io.github.sdsstudios.nvidiagpumonitor.ConnectionManager.Companion.JUICESSH_REQUEST_CODE
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(),
        OnSessionStartedListener,
        OnSessionFinishedListener,
        OnClientStartedListener,
        ConnectionListLoaderFinishedCallback {

    companion object {
        private const val READ_CONNECTIONS = "com.sonelli.juicessh.api.v1.permission.READ_CONNECTIONS"
        private const val OPEN_SESSIONS = "com.sonelli.juicessh.api.v1.permission.OPEN_SESSIONS"
        private const val PERMISSION_REQUEST_CODE = 23
        private const val JUICE_SSH_PACKAGE_NAME = "com.sonelli.juicessh"
    }

    private var mReadConnectionsPerm = false
    private var mOpenSessionsPerm = false

    private val mConnectionManager by lazy {
        ConnectionManager(
                ctx = this,
                mActivitySessionStartedListener = this,
                mActivitySessionFinishedListener = this
        )
    }

    private val mConnectionListAdapter by lazy { ConnectionListAdapter(this) }

    private val mPermissionsGranted
        get() = mReadConnectionsPerm && mOpenSessionsPerm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (isJuiceSSHInstalled()) {

            textViewErrorMessage.setText(R.string.error_must_enable_permissions)

            requestPermissions()

            mConnectionManager.powerUsage.observe(this, Observer {
                textViewPower.setData(it, "W")
            })

            mConnectionManager.temperature.observe(this, Observer {
                textViewTemp.setData(it, "C")
            })

            mConnectionManager.fanSpeed.observe(this, Observer {
                textViewFanSpeed.setData(it, "%")
            })

            mConnectionManager.freeMemory.observe(this, Observer {
                textViewFreeMemory.setData(it, "MB")
            })

            mConnectionManager.usedMemory.observe(this, Observer {
                textViewUsedMemory.setData(it, "MB")
            })

            mConnectionManager.graphicsClock.observe(this, Observer {
                textViewClockGraphics.setData(it, "MHz")
            })

            mConnectionManager.videoClock.observe(this, Observer {
                textViewClockVideo.setData(it, "MHz")
            })

            mConnectionManager.memoryClock.observe(this, Observer {
                textViewClockMemory.setData(it, "MHz")
            })

            if (mPermissionsGranted) {
                onPermissionsGranted()
            }

            buttonConnect.setOnClickListener {
                if (mConnectionListAdapter.count == 0) {
                    Toast.makeText(
                            this,
                            R.string.error_must_have_atleast_one_server,
                            Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                if (mPermissionsGranted) {
                    buttonConnect.applyConnectingStyle()

                    val uuid = mConnectionListAdapter
                            .getConnectionId(spinnerConnectionList.selectedItemPosition)

                    mConnectionManager.toggleConnection(uuid = uuid!!, activity = this)

                } else {
                    requestPermissions()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!isJuiceSSHInstalled()) {
            textViewErrorMessage.setText(R.string.error_must_install_juicessh)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mConnectionManager.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {

        fun isGranted(resultIndex: Int): Boolean {
            return grantResults.isNotEmpty() && grantResults[resultIndex] ==
                    PackageManager.PERMISSION_GRANTED
        }

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                mReadConnectionsPerm = isGranted(0)
                mOpenSessionsPerm = isGranted(1)
            }
        }

        if (mPermissionsGranted) onPermissionsGranted()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == JUICESSH_REQUEST_CODE) {
            mConnectionManager.gotActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String?) {
        buttonConnect.applyDisconnectStyle()
        cardViewLayout.visibility = View.VISIBLE
        spinnerConnectionList.isEnabled = false
    }

    override fun onSessionCancelled() {
        buttonConnect.applyConnectStyle()
    }

    override fun onSessionFinished() {
        buttonConnect.applyConnectStyle()
        cardViewLayout.visibility = View.GONE
        spinnerConnectionList.isEnabled = true
    }

    override fun onClientStarted() {
        buttonConnect.isEnabled = true
    }

    override fun onClientStopped() {
        buttonConnect.isEnabled = false
    }

    override fun onLoaderFinished(newCursor: Cursor?) {
        mConnectionListAdapter.swapCursor(newCursor)
    }

    private fun onPermissionsGranted() {
        mConnectionManager.startClient(onClientStartedListener = this)

        textViewErrorMessage.visibility = View.GONE
        buttonConnect.applyConnectStyle()

        spinnerConnectionList.adapter = mConnectionListAdapter

        supportLoaderManager.initLoader(0, null, ConnectionListLoader(
                mCtx = this,
                mLoaderFinishCallback = this
        ))
    }

    private fun AppCompatTextView.setData(value: Int?, suffix: String) {
        if (value == null) {
            setText(R.string.no_data)
        } else {
            val data = "$value $suffix"
            text = data
        }
    }

    private fun requestPermissions() {
        mReadConnectionsPerm = hasPermission(READ_CONNECTIONS)
        mOpenSessionsPerm = hasPermission(OPEN_SESSIONS)

        if (!mReadConnectionsPerm || !mOpenSessionsPerm) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(READ_CONNECTIONS, OPEN_SESSIONS),
                    PERMISSION_REQUEST_CODE)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun isJuiceSSHInstalled(): Boolean {
        try {
            packageManager.getPackageInfo(JUICE_SSH_PACKAGE_NAME, 0)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
    }
}
