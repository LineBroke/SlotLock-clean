package com.slotlock.slotlock.common;

public final class SlotLockConstants {

    // 私有化构造函数，防止被意外实例化
    private SlotLockConstants() {}

    /*
     * 原版玩家背包的槽位索引范围常数
     */

    // 快捷栏范围 (0 - 8)
    public static final int HOTBAR_START = 0;
    public static final int HOTBAR_END = 8;

    // 主背包范围 (9 - 35)
    public static final int MAIN_INV_START = 9;
    public static final int MAIN_INV_END = 35;

    // 玩家背包总最大索引
    public static final int PLAYER_INV_MAX = 35;
}
