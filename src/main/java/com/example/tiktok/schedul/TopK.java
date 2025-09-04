package com.example.tiktok.schedul;

import com.example.tiktok.entity.vo.HotVideo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

/**
 * 最小堆
 */
public class TopK {
    // 堆的最大数量
    private int k;

    private Queue<HotVideo> queue;

    public TopK(int k, Queue<HotVideo> queue) {
        this.k = k;
        this.queue = queue;
    }

    public void add(HotVideo hotVideo) {
        // 如果堆还没有满，直接推入
        if (queue.size() < k) {
            queue.add(hotVideo);
        } else if (queue.peek().getHot() < hotVideo.getHot()) {
            // 如果视频的热度比最小堆的堆顶视频热度还大，替换
            queue.poll();
            queue.add(hotVideo);
        }
    }

    public List<HotVideo> get() {
        final ArrayList<HotVideo> list = new ArrayList<>(queue.size());

        while (!queue.isEmpty()) {
            list.add(queue.poll());
        }
        Collections.reverse(list);

        return list;
    }
}
