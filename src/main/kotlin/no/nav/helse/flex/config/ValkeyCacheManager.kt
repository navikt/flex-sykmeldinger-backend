import io.valkey.JedisPool
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import java.util.concurrent.Callable

@Component
class ValkeyCacheManager(
    private val jedisPool: JedisPool,
) : CacheManager {
    override fun getCache(name: String): Cache = ValkeyCache(name, jedisPool)

    override fun getCacheNames(): MutableCollection<String> {
        jedisPool.resource.use { jedis ->
            val cacheNames = jedis.keys("*")
            return cacheNames.toMutableList()
        }
    }
}

class ValkeyCache(
    private val name: String,
    private val jedisPool: JedisPool,
) : Cache {
    override fun getName(): String = name

    override fun getNativeCache(): Any = jedisPool // Expose the JedisPool

    override fun get(key: Any): Cache.ValueWrapper? {
        jedisPool.resource.use { jedis ->
            val value = jedis.get(key.toString())
            return value?.let { SimpleValueWrapper(it) }
        }
    }

    override fun <T : Any?> get(
        key: Any,
        type: Class<T>?,
    ): T? {
        jedisPool.resource.use { jedis ->
            val value = jedis.get(key.toString())
            return value?.let {
                if (type != null && type.isAssignableFrom(String::class.java)) {
                    type.cast(it)
                } else {
                    null
                }
            }
        }
    }

    override fun <T : Any?> get(
        key: Any,
        valueLoader: Callable<T>,
    ): T? {
        val cachedValue = get(key)
        @Suppress("UNCHECKED_CAST")
        return cachedValue?.get() as? T ?: valueLoader.call().also { put(key, it!!) }
    }

    override fun put(
        key: Any,
        value: Any?,
    ) {
        if (value != null) {
            jedisPool.resource.use { jedis ->
                jedis.set(key.toString(), value.toString())
            }
        }
    }

    override fun putIfAbsent(
        key: Any,
        value: Any?,
    ): Cache.ValueWrapper? {
        jedisPool.resource.use { jedis ->
            val existingValue = jedis.get(key.toString())
            if (existingValue == null && value != null) {
                jedis.setnx(key.toString(), value.toString())
                return SimpleValueWrapper(value)
            }
            return existingValue?.let { SimpleValueWrapper(it) }
        }
    }

    override fun evict(key: Any) {
        jedisPool.resource.use { jedis ->
            jedis.del(key.toString())
        }
    }

    override fun clear() {
        // Clearing the entire cache can be dangerous in a shared Valkey instance.
        // Consider iterating through keys and deleting them selectively.
        jedisPool.resource.use { jedis ->
            jedis.flushDB()
        }
    }

    class SimpleValueWrapper(
        private val value: Any,
    ) : Cache.ValueWrapper {
        override fun get(): Any = value
    }
}
