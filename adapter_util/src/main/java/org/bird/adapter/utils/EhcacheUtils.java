package org.bird.adapter.utils;


import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import java.io.File;

/**
 * @author bird
 * @date 2021-7-2 13:53
 **/
@Slf4j
public class EhcacheUtils {
    private static final String EHCACHE_NAME = new File("/config","ehcache.xml").toString();
    private final Cache cache;


    public EhcacheUtils(String cacheName) {
        String confPath = System.getProperty("user.dir");
        String confName = confPath + EHCACHE_NAME;
        CacheManager cacheManager = CacheManager.create(confName);
        cache = cacheManager.getCache(cacheName);
    }


    public final void Put(final Object key, final Object value) {
        Element element = new Element(key, value);
        cache.put(element);
    }

    public final Object Get(Object key) {
        Element element = cache.get(key);
        if(element != null) {
            if(element.isExpired()){
                cache.remove(key);
                return null;
            }
            return element.getObjectValue();
        } else return null;
    }

    public final String GetString(Object key) {
        Object value = Get(key);
        if(value != null) {
            return value.toString();
        } else {
            return "";
        }
    }

    public final void removeAll() {
        cache.removeAll();
    }

    public final boolean Exists(Object key) {
        Element element = cache.get(key);
        if(element != null) {
            if(element.isExpired()) {
                cache.remove(key);
            }
        }
        return cache.isKeyInCache(key);
    }

}
