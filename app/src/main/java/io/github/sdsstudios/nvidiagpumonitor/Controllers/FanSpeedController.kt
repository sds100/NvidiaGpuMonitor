package io.github.sdsstudios.nvidiagpumonitor.Controllers

import android.arch.lifecycle.MutableLiveData
import android.content.Context

/**
 * Created by Seth on 05/03/18.
 */

class FanSpeedController(
        ctx: Context,
        liveData: MutableLiveData<Int>
) : BaseController(ctx, liveData) {

    override val regex = Regex("""\d+""")

    override val command = "nvidia-smi --query-gpu=fan.speed --format=csv"

    override fun convertDataToInt(data: String): Int {
        return data.toInt()
    }
}