import java.util.Map;
import java.util.HashMap;
class LockPlain {
  private Map<String, String> dictionary = new HashMap<String, String>();
  private final java.util.concurrent.locks.ReadWriteLock dictionaryLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
  
  LockPlain() {
    super();
  }
  
  public @lombok.WriteLock("dictionaryLock") @java.lang.SuppressWarnings("all") void put(final @lombok.Validate.NotEmpty @lombok.Sanitize.With("checkKey") String key, final String value) {
    if ((key == null))
        {
          throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "key", 1));
        }
    if (key.isEmpty())
        {
          throw new java.lang.IllegalArgumentException(java.lang.String.format("The validated object \'%s\' (argument #%s) is empty", "key", 1));
        }
    final String sanitizedKey = checkKey(key);
    this.dictionaryLock.writeLock().lock();
    try 
      {
        dictionary.put(sanitizedKey, value);
      }
    finally
      {
        this.dictionaryLock.writeLock().unlock();
      }
  }
  
  public @lombok.ReadLock("dictionaryLock") @java.lang.SuppressWarnings("all") String get(final @lombok.Validate.NotEmpty @lombok.Sanitize.With("checkKey") String key) {
    if ((key == null))
        {
          throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "key", 1));
        }
    if (key.isEmpty())
        {
          throw new java.lang.IllegalArgumentException(java.lang.String.format("The validated object \'%s\' (argument #%s) is empty", "key", 1));
        }
    final String sanitizedKey = checkKey(key);
    this.dictionaryLock.readLock().lock();
    try 
      {
        return dictionary.get(sanitizedKey);
      }
    finally
      {
        this.dictionaryLock.readLock().unlock();
      }
  }
  
  private String checkKey(final String key) {
    return key;
  }
}