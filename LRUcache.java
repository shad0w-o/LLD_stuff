import java.util.*;
import java.time.Instant;

class Node<K, V> {
    K key;
    V value;
    Instant expiresAt;
    Node<K, V> prev, next;
    Node(K key, V value, Instant expiresAt) {
        this.key = key;
        this.value = value;
        this.expiresAt = expiresAt;
    }
}

class LRU<K, V> {
    private final int capacity;
    private final long ttl; // in seconds
    private final Node<K, V> head;
    private final Node<K, V> tail;
    private final HashMap<K, Node<K, V>> map;
    private final PriorityQueue<Node<K, V>> expQueue;

    public LRU(int capacity, long ttl) {
        if(capacity <= 0) throw new IllegalArgumentException("capacity must be positive, got: " + capacity);
        if(ttl <= 0) throw new IllegalArgumentException("ttl must be positive, got: " + ttl);
        this.capacity = capacity;
        this.ttl = ttl;
        head = new Node<>(null, null, null);
        tail = new Node<>(null, null, null);
        map = new HashMap<>();
        expQueue = new PriorityQueue<>(Comparator.comparing(node -> node.expiresAt));
        head.next = tail;
        tail.prev = head;
    }

    public synchronized V get(K key) {
        Node<K, V> node = map.get(key);
        if(node == null) return null;

        Instant now = Instant.now();
        if(now.isAfter(node.expiresAt)) {
            removeNode(node); // remove() on a priority queue takes O(n), feels like its better to leave it stale and do a lazy removal but idk i am leaving it this way
            return null;
        }

        moveToFront(node);
        return node.value;
    }

    public synchronized void put(K key, V value) {
        Node<K, V> node = map.get(key);
        if(node != null) {
            expQueue.remove(node);
            node.value = value;
            node.expiresAt = Instant.now().plusSeconds(ttl); // resets on write
            expQueue.offer(node);
            moveToFront(node);
            return;
        }

        if(map.size() == capacity) {
            purgeExpired();
            if (map.size() == capacity) evict();
        }

        Instant expiresAt = Instant.now().plusSeconds(ttl);
        Node<K, V> node = new Node<>(key, value, expiresAt);
        map.put(key, node);
        expQueue.offer(node);
        addFirst(node);
    }

    private void evict() {
        Node<K, V> node = tail.prev;
        removeNode(node);
    }

    private void removeNode(Node<K, V> node) {
        removeFromList(node);
        map.remove(node.key);
        expQueue.remove(node);
    }

    private void addFirst(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void moveToFront(Node<K, V> node) {
        removeFromList(node);
        addFirst(node);
    }

    private void removeFromList(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.next = null;
        node.prev = null;
    }

    private void purgeExpired() {
//        Instant now = Instant.now();
//        Node<K, V> node = head.next;
//
//        while(node != tail) {
//            Node<K, V> next = node.next;
//            if(now.isAfter(node.expiresAt)) {
//                remove(node);
//                map.remove(node.key);
//            }
//            node = next;
//        }

        Instant now = Instant.now();
        while(!expQueue.isEmpty() && now.isAfter(expQueue.peek().expiresAt)) {
            Node<K, V> node = expQueue.poll();
            removeFromList(node);
            map.remove(node.key);
        }
    }
}

class ShardedLRU<K, V> {
    private LRU<K, V>[] shards;

    public ShardedLRU(int shardCount, int perShardCapacity, long ttl) {
        if(shardCount <= 0)
            throw new IllegalArgumentException("shard count must be positive, got: " + shardCount);
        if(perShardCapacity <= 0)
            throw new IllegalArgumentException("capacity must be positive, got: " + perShardCapacity);
        if(ttl <= 0)
            throw new IllegalArgumentException("ttl must be positive, got: " + ttl);

        shards = new LRU[shardCount];
        for (int i = 0; i < shardCount; i++) shards[i] = new LRU<>(perShardCapacity, ttl);
    }

    private LRU<K, V> getShard(K key) {
        int idx = (key.hashCode() & 0x7fffffff) % shards.length;
        return shards[idx];
    }

    public V get(K key) { return getShard(key).get(key); }

    public void put(K key, V value) { getShard(key).put(key, value); }
}

//public class LRUcache {
//    public static void main(String[] args) {
//        LRU<Integer, String> lru = new LRU<>(3);
//        lru.put(1, "Hello");
//        lru.put(2, "world");
//        lru.put(3, "!");
//        lru.put(4, "Hell");
//        System.out.println(lru.get(1));
//    }
//}