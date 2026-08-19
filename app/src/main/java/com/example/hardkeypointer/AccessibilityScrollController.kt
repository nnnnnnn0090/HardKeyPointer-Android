/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.view.accessibility.AccessibilityNodeInfo

/** 対象アプリが公開している標準スクロール操作を実行します。 */
class AccessibilityScrollController(
    private val rootProvider: () -> AccessibilityNodeInfo?,
    private val rotationProvider: () -> Int
) {
    /** 標準アクションが実行できた場合に true を返します。 */
    fun scroll(direction: PointerDirection): Boolean {
        val root = rootProvider() ?: return false
        val actionId = actionId(direction.rotated(rotationProvider()))
        val node = findScrollableNode(root, actionId) ?: return false
        return runCatching { node.performAction(actionId) }.getOrDefault(false)
    }

    /** 指定アクションを持つノードを、フォーカスと階層から検索します。 */
    private fun findScrollableNode(
        root: AccessibilityNodeInfo,
        actionId: Int
    ): AccessibilityNodeInfo? {
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            ?: root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && hasAction(focused, actionId)) return focused
        return findScrollableNodeDepthFirst(root, actionId)
    }

    /** ノード自身と子ノードを深さ優先で調べます。 */
    private fun findScrollableNodeDepthFirst(
        node: AccessibilityNodeInfo,
        actionId: Int
    ): AccessibilityNodeInfo? {
        if (hasAction(node, actionId)) return node
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            findScrollableNodeDepthFirst(child, actionId)?.let { return it }
        }
        return null
    }

    /** ノードが指定された標準操作を公開しているか確認します。 */
    private fun hasAction(node: AccessibilityNodeInfo, actionId: Int): Boolean =
        node.actionList.any { it.id == actionId }

    /** 論理方向をAccessibilityNodeInfoの操作IDへ変換します。 */
    private fun actionId(direction: PointerDirection): Int = when (direction) {
        PointerDirection.UP -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id
        PointerDirection.DOWN -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id
        PointerDirection.LEFT -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.id
        PointerDirection.RIGHT -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.id
    }
}
