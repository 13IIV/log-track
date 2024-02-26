package org.slf4j;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.slf4j.spi.MDCAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TtlMDCAdapter implements MDCAdapter {

    //MDC内部使用ThreadLocal实现， traceId只在本线程有效 子线程和下游服务会丢失
    //TransmittableThreadLocal 用于解决在使用线程池等会池化复用线程的执行组件情况下，
    // 提供ThreadLocal值的传递功能，解决异步执行时上下文传递的问题
    private final ThreadLocal<Map<String, String>> copyOnInheritThreadLocal = new TransmittableThreadLocal<>();

    private static final int WRITE_OPERATION = 1;
    private static final int MAP_COPY_OPERATION = 2;

    private static TtlMDCAdapter mtcMDCAdapter;

    /**
     * keeps track of the last operation performed
     */
    private final ThreadLocal<Integer> lastOperation = new ThreadLocal<>();

    static {
        mtcMDCAdapter = new TtlMDCAdapter();
        MDC.mdcAdapter = mtcMDCAdapter;
    }
    //返回重写后的mdcAdapter实例
    public static MDCAdapter getInstance() {
        return mtcMDCAdapter;
    }
    
    //get last operation
    private Integer getAndSetLastOperation(int op) {
        Integer lastOp = lastOperation.get();
        lastOperation.set(op);
        return lastOp;
    }
    
    private static boolean wasLastOperationReadOrNot(Integer lastOp){
        return lastOp == null || lastOp == MAP_COPY_OPERATION;
    }
    
    private Map<String,String> duplicateAndInsertNewMap(Map<String,String> oldMap) {
        Map<String,String> newMap = Collections.synchronizedMap(new HashMap<>());
        if (oldMap != null)  {
            // we don't want the parent thread modifying oldMap while we are iterating over it
            // 在遍历oldMap时 加锁 避免 父线程对其进行修改
            synchronized (oldMap) {
                newMap.putAll(oldMap);
            }
        }
        copyOnInheritThreadLocal.set(newMap);
        return newMap;
    }

    /**
     * Put a context value (the val parameter) as identified with the key parameter into the current thread's context map. 
     * The key parameter cannot be null. The val parameter can be null only if the underlying implementation supports it.
         * If the current thread does not have a context map it is created as a side effect of this call.
     *
     * @throws IllegalArgumentException in case the "key" parameter is null
     */
    @Override
    public void put(String key, String val) {
        if (key == null) {
            throw new IllegalArgumentException("key can't be null");
        }
        Map<String,String> oldMap = copyOnInheritThreadLocal.get();
        Integer lastOp = getAndSetLastOperation(WRITE_OPERATION);
        if (wasLastOperationReadOrNot(lastOp) || oldMap == null) {
            Map<String,String> newMap = duplicateAndInsertNewMap(oldMap);
            newMap.put(key,val);
        } else {
            oldMap.put(key,val);
        }
    }

    /**
     * Get the context identified by the key parameter. The key parameter cannot be null.
     * Returns:
     * the string value identified by the key parameter.
     * @param key
     * @return
     */
    @Override
    public String get(String key) {
        final Map<String, String> map = copyOnInheritThreadLocal.get();
        if ((map != null) && (key != null)) {
            return map.get(key);
        } else {
            return null;
        }
    }

    /**
     * Remove the context identified by the key parameter. The key parameter cannot be null.
     * This method does nothing if there is no previous value associated with key.
     * @param key
     */
    @Override
    public void remove(String key) {
        if (key == null) return;
        Map<String,String> oldMap = copyOnInheritThreadLocal.get();
        if (oldMap == null) return;
        Integer lastOp = getAndSetLastOperation(WRITE_OPERATION);
        if (wasLastOperationReadOrNot(lastOp)) {
            Map<String,String> newMap = duplicateAndInsertNewMap(oldMap);
            newMap.remove(key);
        } else {
            oldMap.remove(key);
        }
    }

    /**
     * Clear all entries in the MDC.
     */
    @Override
    public void clear() {
        lastOperation.set(WRITE_OPERATION);
        copyOnInheritThreadLocal.remove();
    }

    /**
     * Get the current thread's MDC as a map. This method is intended to be used
     * internally.
     */
    public Map<String,String> getPropertyMap() {
        lastOperation.set(MAP_COPY_OPERATION);
        return copyOnInheritThreadLocal.get();
    }

    /**
     * Returns the keys in the MDC as a {@link Set}. The returned value can be
     * null.
     */
    public Set<String> getKeys() {
        Map<String,String> map = getPropertyMap();
        if (map != null) {
            return map.keySet();
        } else {
            return null;
        }
    }

    /**
     * Return a copy of the current thread's context map. Returned value may be null.
     */
    @Override
    public Map<String, String> getCopyOfContextMap() {
        Map<String,String> hashMap = copyOnInheritThreadLocal.get();
        if(hashMap == null) {
            return null;
        } else {
            return new HashMap<>(hashMap);
        }
    }

    @Override
    public void setContextMap(Map<String, String> contextMap) {
        lastOperation.set(WRITE_OPERATION);
        Map<String,String> newMap = Collections.synchronizedMap(new HashMap<>());
        newMap.putAll(contextMap);
        //出于序列化的目的 让新map 取代 旧map
        copyOnInheritThreadLocal.set(newMap);
    }
}
