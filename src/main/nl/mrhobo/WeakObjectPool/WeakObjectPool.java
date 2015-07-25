package nl.mrhobo.WeakObjectPool;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Maintains a thread-safe pool of weak-referenced identifiable objects. Objects
 * with the same identifier will be pooled. An info object can be added to each
 * object allowing the object to be sort-of extended without changing the object. 
 * 
 *  <b>Weak-referenced</b> - Objects that are added to this pool may be collected by
 *  the garbage collector.
 *  <b>Identifiable</b> - It is assumed that the objects stored in this pool can
 *  be identified by some kind of an identifier. Objects of the same identifier
 *  will then be pooled.
 *
 * @param <ID> The type of the object identifier, the key to the object T
 * @param <T> The type of the object stored in the pool
 * @param <I> An additional info object, which can store information about the
 * object that cannot be stored in the object itself.
 *
 * @author Lodewijk Bogaards, www.mrhobo.nl
 */
public class WeakObjectPool<ID, T, I>
{
    private static final int DEFAULT_INITIAL_CAPACITY = 1000;

    /**
     *  the pool based on the id of the data model object
     */
	private final HashMap<ID, List<PooledObjectRef>> pool; 

	/**
	 *  the queue in which the dead objects will magically appear
	 */
    private final ReferenceQueue<T> deadObjects = new ReferenceQueue<T>();
    
    /**
     *  An index to keep track of which pool object was stored where. This aids in speeding up
     *  the cleaning process of the references.
     */
    private final IdentityHashMap<WeakReference<T>, PoolIndex> poolIndex; 
    	    
    private class PooledObjectRef extends Obj<WeakReference<T>, I>
    {
		private static final long serialVersionUID = 3000628487640557589L;
    }
    
    private class PoolIndex 
    {
    	public ID ojbectId;
    	public List<PooledObjectRef> refs;
    	public int index;
    }
    
    public WeakObjectPool()
	{
    	this(DEFAULT_INITIAL_CAPACITY);
	}
    
    public WeakObjectPool(int initialCapactiy)
	{
    	pool = new HashMap<ID, List<PooledObjectRef>>(initialCapactiy);
    	poolIndex = new IdentityHashMap<WeakReference<T>, PoolIndex>(initialCapactiy);
	}

    public synchronized  void add(ID objectId, Obj<T, I> obj)
    {
    	add(objectId, obj.getObject(), obj.getInfo());
    }
    
    /**
     * Adds object to the pool without info (info=null)
     * @param objectId the id of the object
     * @param obj the object
     */
    public synchronized  void add(ID objectId, T obj)
    {
    	add(objectId, obj, null);
    }
        
    /**
     * Adds a weak reference of this object to the pool.
     * @param objectId the of the object
     * @param obj the object
     */
    public synchronized  void add(ID objectId, T obj, I info) 
    {
    	// do this regularly
    	removeDeadObjects();
    	// do this regularly
   	
    	List<PooledObjectRef> poolObjs = pool.get(objectId);
    	if (poolObjs == null)
    		poolObjs = new ArrayList<PooledObjectRef>();
    	else
    	{
    		for (PooledObjectRef poolObj : poolObjs)
    		{
    			if (poolObj != null && obj == poolObj.getObject().get())
    				// duplicate entry, do not add
    				return;
    		}
    	}
    		
    	// add reference to pool
    	PooledObjectRef poolObj = new PooledObjectRef();
    	WeakReference<T> weakRef = new WeakReference<T>(obj, deadObjects);
    	poolObj.setObject(weakRef);
    	poolObj.setInfo(info);
    	poolObjs.add(poolObj);
    	pool.put(objectId, poolObjs);
    	
    	// add pool index to reference index
    	PoolIndex pIdx = new PoolIndex();
    	pIdx.refs = poolObjs;
    	pIdx.ojbectId = objectId;
    	pIdx.index = poolObjs.size()-1;
    	poolIndex.put(weakRef, pIdx);
    }
    
    /**
     * Gets all stored references to still existing objects from the object pool  
     * @param objectId the id of the objects for which the reference need to be gotten
     */
    public synchronized List<Obj<T, I>> get(ID objectId)
    {
    	// do this regularly
    	removeDeadObjects();
    	// do this regularly

    	List<PooledObjectRef> poolObjs = pool.get(objectId);
    	if (poolObjs == null)
    		return Collections.emptyList();

    	// copy hard refs
    	List<Obj<T, I>> objList = new ArrayList<Obj<T, I>>(poolObjs.size());
    	Iterator<PooledObjectRef> poolObjRefIt = poolObjs.iterator();
    	while (poolObjRefIt.hasNext())
    	{
    		PooledObjectRef poolObj = poolObjRefIt.next();
    		if (poolObj == null)
    			continue;
    		
    		T object = poolObj.getObject().get();
			if (object != null)
			{
				// add active references
				objList.add(
						new Obj<T, I>(
								object, 
								poolObj.getInfo()
							)
					);
			}
    	}   	
    	    	
    	return objList;
    }
        
    /**
     * Convenience method that get the objects without
     * additional info object
     * @param objectId the id of the objects for which the reference need to be gotten
     * @return the objects without additional info object
     */
    public synchronized List<T> getObjects(ID objectId)
    {
    	List<Obj<T, I>> objs = get(objectId);
    	List<T> rtn = new ArrayList<T>(objs.size());
    	for (Obj<T, I> obj : objs)
    	{
    		rtn.add(obj.getObject());
    	}
    	return rtn;
    }
    
    public synchronized T getFirstObject(ID objectId)
    {
    	List<Obj<T, I>> objs = get(objectId);
    	if (objs.size() > 0)
    		return objs.get(0).getObject();
    	else
    		return null;
    }       
    
    /**
     * Refers to getReferenceCount() 
     * @see WeakObjectPool#getReferenceCount()
     */
    public int size()
    {
    	return getReferenceCount();
    }
    
    /**
     * @return the number of uniquely identified objects are stored in the pool at
     * this moment
     */
    public synchronized int getObjectIdCount()
    {
    	// do this regularly
    	removeDeadObjects();
    	// do this regularly

    	return pool.size();
    }

    /**
     * @return the number of valid references held in the pool at this moment
     */
    public synchronized int getReferenceCount()
    {
    	// do this regularly
    	removeDeadObjects();
    	// do this regularly
    	
    	int count = 0;
    	for(List<PooledObjectRef> poolObjs : pool.values())
    	{
    		// count all non-null objs
    		for(PooledObjectRef pooledObj : poolObjs)
    		{
    			if (pooledObj != null)
    				count++;
    		}
    	}
    	return count;
    }

	/**
	 * Called internally for removing dead object references from the pool. This 
	 * method was sped up by the refIndex which stores a list of which references
	 * lists exist for which object identifiers.
	 */
	private void removeDeadObjects()
	{
		@SuppressWarnings("rawtypes")
		Reference weakReference = deadObjects.poll();
		while(weakReference != null)
		{
			// search the index of the reference in the pool by the reference
			PoolIndex pIdx = poolIndex.get(weakReference);
			
			// set object to null instead of removing it, so other indices
			// do not corrupt
			pIdx.refs.set(pIdx.index, null);
			boolean allNull = true;
			for (PooledObjectRef ref :  pIdx.refs)
			{
				if (ref != null)
				{
					allNull = false;
					break;
				}
			}
			if (allNull)
				pool.remove(pIdx.ojbectId);
			
			poolIndex.remove(weakReference);

			weakReference = deadObjects.poll();
		}
	}

	@Override
	public String toString()
	{
		return super.toString() +"[object id count="+ getObjectIdCount() +", reference count="+ getReferenceCount() +"]";
	}

	class Obj<OBJECT_TYPE, INFO_TYPE> implements Serializable
	{
		private static final long serialVersionUID = -1764960178220911377L;

		private OBJECT_TYPE object;
		private INFO_TYPE info;
		
		public Obj()
		{
			
		}

		public Obj(OBJECT_TYPE object, INFO_TYPE info)
		{
			this.setObject(object);
			this.setInfo(info);
		}

		public void setObject(OBJECT_TYPE object)
		{
			this.object = object;
		}

		public OBJECT_TYPE getObject()
		{
			return object;
		}

		public void setInfo(INFO_TYPE info)
		{
			this.info = info;
		}

		public INFO_TYPE getInfo()
		{
			return info;
		}
	}

}
