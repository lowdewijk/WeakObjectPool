#Weak Object Pool

## Problem

Did you ever want to track a bunch of objects without referencing them directly in order not to disturb their natural lifecycle using weak references? Have you ever tried to extend Java objects during run-time?

## Solution

*WeakObjectPool*: A pool for holding and extending [weak referenced objects](http://download.oracle.com/javase/1,5.0/docs/api/java/lang/ref/WeakReference.html" title="WeakReference JavaDoc). How does this differ from [Java's WeakHashMap](http://docs.oracle.com/javase/6/docs/api/java/util/WeakHashMap.html)? Several key differences are there, that effecitvely define WeakObjectPool:

 1. WeakHashMap has a weak *keys* not values, like WeakObjectPool.
 1. Its a pool not a map, otherwise also known as a multi-map.
 1. WeakObjectPool allows you to store additional information about a weakly referenced object in the pool for the duration of the lifecycle of the object (a decorator). 
 
#Usage: 

No dependencies required.

```
new WeakObjectPool<KEY, VALUE, DECORATOR>()
```

# Example:

```
/* Create the pool. 
		ID type is String. 
		Object type is Integer.
		Extra data type is String 
		*/
WeakObjectPool<String, Integer, String> pool =          new WeakObjectPool<>();

// make integer object x
Integer x = new Integer(314);
		
// store x in pool with extra info (String)
pool.add("x", x, "X was created on "+ new Date().getTime());
		
// get x out of the pool
System.out.println("Object x = "+ pool.get("x").get(0).getObject());
		System.out.println(pool.get("x").get(0).getInfo());
```

Console output: 

```
Object x = 314
X was created on 1321807672613
```

Now if x is not referenced anymore <i>and</i> the object x was erased by the garbage collector the object will automatically disappear from the pool (that's what weak referenced means). To demonstrate:

```
// get rid of all x references
x=null;
// make sure it's completely gone
System.gc();
System.runFinalization();

// proof it's gone from the pool
System.out.println("References found to x = "+ pool.get("x").size());
```

Console output:

```
References found to x = 0. 
```

## Background

This code is pretty old (written in 2007 with Java 5). I'd recommend having a look at [Gauva](https://github.com/google/guava) to see if any of their collection types fit your needs. The decoration part of this code still seems to be unique though.
