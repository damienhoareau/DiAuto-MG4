package com.andrerinas.openheadunit.vehicle

/**
 * Driver assistance properties of the vehicle that come from the front camera and radar, with the
 * ids the car's CarAdvancedAssistedDrivingManager gives them (generated from the adapter's copy of
 * it). Read-only, for the test screen: what the car publishes while it drives.
 */
internal object AdasProperties {

    class Group(val title: String, val properties: List<Pair<String, Int>>)

    /** The ids hold their type: the kind of value is in bits 16 to 23. */
    const val TYPE_MASK = 0x00FF0000
    const val TYPE_BOOLEAN = 0x00200000
    const val TYPE_INT = 0x00400000
    const val TYPE_FLOAT = 0x00600000

    val groups: List<Group> = listOf(
        Group(
            "Speed limit and road type",
            listOf(
                "AAD_SPEED_ASST_SYS" to 557883649,
                "AAD_SPEED_ASST_MODE" to 557883650,
                "AAD_SPEED_ASST_MODE_FLAG" to 557883651,
                "AAD_SLIF_WARNING" to 557883756,
                "AAD_SLIF_WARNING_VALID" to 557883759,
                "NAVIGATION_CRNT_ROAD_TYPE" to 557883778,
                "NAVIGATION_SPEED_LIMIT_VALUE_STATUS" to 557883779,
                "NAVIGATION_SPEED_LIMIT_VALUE_UNIT" to 557883780,
                "NAVIGATION_SPEED_LIMIT_VALUE" to 559980933,
                "AAD_SPEED_LIMIT_UPDATE_PROMPT_SOUND" to 557883828,
                "AAD_SPEED_LIMIT_MODE" to 557883829,
                "AAD_INTELLIGENT_SPEED_LIMIT" to 557883830,
                "SPD_LMT_WRNNG_FN_STS" to 557883833,
                "NAVIGATION_SPEED_LIMIT_TYPE" to 557883844,
                "NAVIGATION_CONDITIONAL_SPEED_LIMIT_VALUE" to 557883856,
            ),
        ),
        Group(
            "Lane lines",
            listOf(
                "AAD_LANE_DEPARTURE_WARNING" to 557883652,
                "AAD_LANE_ALARM" to 557883653,
                "AAD_LANE_DISPLAY_LANE0_ID" to 557883664,
                "AAD_LANE_DISPLAY_LANE0_LOCATION" to 557883665,
                "AAD_LANE_DISPLAY_LANE0_MARKERTYPE" to 557883666,
                "AAD_LANE_DISPLAY_LANE0_MARKERCOLOR" to 557883667,
                "AAD_LANE_DISPLAY_LANE0_VIEWRANGESTART" to 559980820,
                "AAD_LANE_DISPLAY_LANE0_VIEWRANGEEND" to 559980821,
                "AAD_LANE_DISPLAY_LANE0_LDWFLAG" to 559980822,
                "AAD_LANE_DISPLAY_LANE0_PART0_SYNCFRAMEINDEX" to 557883671,
                "AAD_LANE_DISPLAY_LANE0_C0" to 559980824,
                "AAD_LANE_DISPLAY_LANE0_C1" to 559980825,
                "AAD_LANE_DISPLAY_LANE0_C2" to 559980826,
                "AAD_LANE_DISPLAY_LANE0_C3" to 559980827,
                "AAD_LANE_DISPLAY_LANE0_PART1_SYNCFRAMEINDEX" to 557883676,
                "AAD_LANE_DISPLAY_LANE1_ID" to 557883677,
                "AAD_LANE_DISPLAY_LANE1_LOCATION" to 557883678,
                "AAD_LANE_DISPLAY_LANE1_MARKERTYPE" to 557883679,
                "AAD_LANE_DISPLAY_LANE1_MARKERCOLOR" to 557883680,
                "AAD_LANE_DISPLAY_LANE1_VIEWRANGESTART" to 559980833,
                "AAD_LANE_DISPLAY_LANE1_VIEWRANGEEND" to 559980834,
                "AAD_LANE_DISPLAY_LANE1_LDWFLAG" to 559980835,
                "AAD_LANE_DISPLAY_LANE1_PART0_SYNCFRAMEINDEX" to 557883684,
                "AAD_LANE_DISPLAY_LANE1_C0" to 559980837,
                "AAD_LANE_DISPLAY_LANE1_C1" to 559980838,
                "AAD_LANE_DISPLAY_LANE1_C2" to 559980839,
                "AAD_LANE_DISPLAY_LANE1_C3" to 559980840,
                "AAD_LANE_DISPLAY_LANE1_PART1_SYNCFRAMEINDEX" to 557883689,
            ),
        ),
        Group(
            "Tracked objects (ACC)",
            listOf(
                "AAD_ACC_OBJ_ID" to 557883691,
                "AAD_ACC_OBJ_LONG_RLTV_DIST" to 559980844,
                "AAD_ACC_OBJ_LAT_RLTV_DIST" to 559980845,
                "AAD_ACC_OBJ_LONG_RLTV_SPD" to 559980846,
                "AAD_ACC_OBJ_EXIST_PRBLTY" to 559980847,
                "AAD_ACC_OBS_EXIST_PRBLTY" to 559980848,
                "AAD_ACC_OBJ1_ID" to 557883697,
                "AAD_ACC_OBJ1_LONG_RLTV_DIST" to 559980850,
                "AAD_ACC_OBJ1_LAT_RLTV_DIST" to 559980851,
                "AAD_ACC_OBJ1_LONG_RLTV_SPD" to 559980852,
                "AAD_ACC_OBJ1_EXIST_PRBLTY" to 559980853,
                "AAD_ACC_OBSTACLE1_EXIST_PRBLTY" to 559980854,
                "AAD_ACC_OBJ1_SYNC_CTR" to 557883703,
                "AAD_ACC_OBJ2_ID" to 557883704,
                "AAD_ACC_OBJ2_LONG_RLTV_DIST" to 559980857,
                "AAD_ACC_OBJ2_LAT_RLTV_DIST" to 559980858,
                "AAD_ACC_OBJ2_LONG_RLTV_SPD" to 559980859,
                "AAD_ACC_OBJ2_EXIST_PRBLTY" to 559980860,
                "AAD_ACC_OBSTACLE2_EXIST_PRBLTY" to 559980861,
                "AAD_ACC_OBJ2_SYNC_CTR" to 557883710,
                "AAD_ACC_OBJ3_ID" to 557883711,
                "AAD_ACC_OBJ3_LONG_RLTV_DIST" to 559980864,
                "AAD_ACC_OBJ3_LAT_RLTV_DIST" to 559980865,
                "AAD_ACC_OBJ3_LONG_RLTV_SPD" to 559980866,
                "AAD_ACC_OBJ3_EXIST_PRBLTY" to 559980867,
                "AAD_ACC_OBSTACLE3_EXIST_PRBLTY" to 559980868,
                "AAD_ACC_OBJ3_SYNC_CTR" to 557883717,
                "AAD_ACC_OBJ_SYNC_CTR" to 557883747,
            ),
        ),
        Group(
            "Collision warning",
            listOf(
                "AAD_FRONT_ALARM" to 557883658,
                "AAD_FCW_OBJ_FCWA" to 557883718,
                "AAD_FCW_OBJ_PEDTRNCOLWRNNGA" to 557883719,
                "AAD_FCW_OBJ_FCWTIMETOCOL" to 559980872,
                "AAD_FCW_OBJ_OBSDATATMSP" to 559980873,
                "AAD_FCW_OBJ_DNGRSOBJID" to 557883722,
                "AAD_FCW_OBJ_DNGRSOBJLONGRLTVDIST" to 559980875,
                "AAD_FCW_OBJ_DNGRSOBJLATRLTVDIST" to 559980876,
                "AAD_FCW_OBJ_DNGRSOBJLONGRLTVSPD" to 559980877,
                "AAD_FCW_OBJ_DNGRSOBJLATRLTVSPD" to 559980878,
                "AAD_FCW_OBJ_DNGRSPEDTRNID" to 557883727,
                "AAD_FCW_OBJ_DNGRSPEDTRNLONGRLTVDIST" to 559980880,
                "AAD_FCW_OBJ_DNGRSPEDTRNLATRLTVDIST" to 559980881,
                "AAD_FCW_OBJ_DNGRSPEDTRNLONGRLTVSPD" to 559980882,
                "AAD_FCW_OBJ_DNGRSPEDTRNLATRLTVDSPD" to 559980883,
                "AAD_FCW_WARNING_LEVEL_SIGNAL" to 557883745,
                "AAD_FCW_SYSTEM_FAILURE" to 557883746,
                "AAD_FCW_OBJ_OBSDATASYNCCTR" to 557883748,
                "AAD_FCW_OBJ_DNGRSOBJSYNCCTR" to 557883749,
                "AAD_FCW_OBJ_DNGRSPEDTRNSYNCCTR" to 557883757,
            ),
        ),
    )
}
