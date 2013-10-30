package com.netflix.zuul.proxy.core;

import com.netflix.zuul.proxy.framework.api.AttachedObjectContainer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: root
 * Date: 10/8/13
 * Time: 10:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class AttachedObjectContainerNetty implements AttachedObjectContainer {
    private static final Logger LOG = LoggerFactory.getLogger(AttachedObjectContainerNetty.class);

    private static ChannelLocal<AttachedObjectContainerNetty> CONTAINER = new ChannelLocal<>();

    private URI route;
    private Map<String,Object> attachedObjects = new HashMap<>();

    public void attachObject (String name, Object object) {
        attachedObjects.put(name, object);
        LOG.debug("attached object {} : {}", name, object);
    }

    @Override
    public <T extends Object> T detachObject(String name, Class<T> type) {
        if (attachedObjects.containsKey(name)) {
            Object object = attachedObjects.remove(name);
            if (type.isAssignableFrom(object.getClass())) {
                return type.cast(object);
            } else {
                throw new ClassCastException(String.format("%s cannot be cast to %s", object.getClass().getSimpleName(), type.getSimpleName()));
            }
        }

        return null;
    }

    @Override
    public <T extends Object> T attachedObject(String name, Class<T> type) {
        if (attachedObjects.containsKey(name)) {
            Object object = attachedObjects.get(name);
            if (type.isAssignableFrom(object.getClass())) {
                return type.cast(object);
            } else {
                throw new ClassCastException(String.format("%s cannot be cast to %s", object.getClass().getSimpleName(), type.getSimpleName()));
            }
        }

        return null;
    }

    public static AttachedObjectContainer containerFor (ChannelHandlerContext context) {
        Object containedObject = CONTAINER.get(context.getChannel());

        if (containedObject == null) {
            AttachedObjectContainerNetty newContainedObject = new AttachedObjectContainerNetty();
            CONTAINER.set(context.getChannel(), newContainedObject);
            return newContainedObject;
        } else if (containedObject instanceof AttachedObjectContainerNetty) {
            return (AttachedObjectContainerNetty) containedObject;
        } else {
            throw new IllegalStateException("found invalid context attachment, framework error");
        }

    }
}
