import java.util.*;

class Node<K, V> {
    K key;
    V value;
    Node<K, V> prev, next;
    Node(K key, V value) {
        this.key = key;
        this.value = value;
    }
}

class LRU<K, V> {
    private final int capacity;
    private final Node<K, V> head;
    private final Node<K, V> tail;
    private HashMap<K, Node<K, V>> map;

    public LRU(int capacity) {
        if(capacity <= 0) throw new IllegalArgumentException("capacity must be positive, got: " + capacity);
        this.capacity = capacity;
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        map = new HashMap<>();
        head.next = tail;
        tail.prev = head;
    }

    public synchronized V get(K key) {
        Node<K, V> node = map.get(key);
        if(node == null) return null;

        moveToFront(node);
        return node.value;
    }

    public synchronized void put(K key, V value) {
        if(map.containsKey(key)) {
            Node<K, V> node = map.get(key);
            node.value = value;
            moveToFront(node);
        } else {
            if(map.size() == capacity) evict();
            Node<K, V> node = new Node<>(key, value);
            map.put(key, node);
            addFirst(node);
        }
    }

    private void evict() {
        Node<K, V> node = tail.prev;
        remove(node);
        map.remove(node.key);
    }

    private void addFirst(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void moveToFront(Node<K, V> node) {
        remove(node);
        addFirst(node);
    }

    private void remove(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.next = null;
        node.prev = null;
    }
}

public class LRUcache {
    public static void main(String[] args) {
        LRU<Integer, String> lru = new LRU<>(3);
        lru.put(1, "Hello");
        lru.put(2, "world");
        lru.put(3, "!");
        lru.put(4, "Hell");
        System.out.println(lru.get(1));
    }
}