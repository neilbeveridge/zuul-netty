package com.netflix.zuul.proxy.framework.api;

/**
 * Created with IntelliJ IDEA.
 * User: root
 * Date: 10/8/13
 * Time: 10:30 AM
 * To change this template use File | Settings | File Templates.
 */
public interface AttachedObjectContainer {

    /**
     * Attach an arbitrary object to this request which will be available to all subsequent handlers in the pipeline.
     * Attached objects will NOT be transmitted to the target host but WILL be available to the response processing pipeline.
     * @param name identifier for this object, used to fetch and detach the object from the request.
     * @param object object to store.
     */
    void attachObject (String name, Object object);

    /**
     * Retrieve and remove an attached object from the pipeline scope. It will no longer be available to other handlers in the pipeline.
     * Returns null if an object is not found for the given name.
     * @param name name of the attached object which must match the name used to attach the object
     * @param type the class type of the object - the framework will attempt to cast the object for convenience
     * @param <T> the type of the object to return
     * @return attached object for given name, cast to the requested type
     * @throws ClassCastException if the stored object cannot be cast to the provided type
     */
    <T extends Object> T detachObject (String name, Class<T> type);

    /**
     * Retrieve an attached object from the pipeline scope.
     * Returns null if an object is not found with a matching name AND type.
     * @param name name of the attached object which must match the name used to attach the object
     * @param type the class type of the object - the framework will attempt to cast the object for convenience
     * @param <T> the type of the object to return
     * @return attached object for given name, cast to the requested type
     * @throws ClassCastException if the stored object cannot be cast to the provided type
     */
    <T extends Object> T attachedObject (String name, Class<T> type);

}
