package com.aitusoftware.transport.memory;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

public final class BufferUtil
{
    private BufferUtil()
    {
    }

    public static long mappedBufferCount()
    {
        final List<BufferPoolMXBean> beans = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
        for (BufferPoolMXBean bean : beans)
        {
            if (bean.getName().equals("mapped"))
            {
                return bean.getCount();
            }
        }
        throw new RuntimeException("Could not find number of mapped buffers");
    }
}