package com.fc.v2.service.support;

/**
 * 建档五道筛拒收：各报各的错，不许合并成一句「数据不对」。
 *
 * screen 指明栽在第几道筛（1点位码／2挂靠／3数量／4名额／5落笔），
 * message 是一句句点名的原话（哪一栏不合点哪一栏的名）。
 *
 * @author fuce
 * @date 2026-10-06
 */
public class LedgerRejectException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    /** 栽在第几道筛（1..5） */
    private final int screen;

    public LedgerRejectException(int screen, String message) {
        super("第" + screen + "道筛没过：" + message);
        this.screen = screen;
    }

    public int getScreen() {
        return screen;
    }
}
