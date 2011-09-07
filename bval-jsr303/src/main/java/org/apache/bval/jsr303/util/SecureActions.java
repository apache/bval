/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bval.jsr303.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.bval.util.PrivilegedActions;

/**
 * Description: utility methods to perform actions with AccessController or without.<br/>
 */
public class SecureActions extends PrivilegedActions {

    /**
     * Create a privileged action to get the context classloader of the current thread.
     *
     * @see Thread#getContextClassLoader()
     */
    public static PrivilegedAction<ClassLoader> getContextClassLoader()
    {
        return SecureActions.GetContextClassLoader.instance;
    }


    /**
     * Create a privileged action to get the named field declared by the specified class.
     * The result of the action will be {@code null} if there is no such field.
     */
    public static PrivilegedAction<Field> getDeclaredField(final Class<?> clazz, final String fieldName) {
        return new PrivilegedAction<Field>() {
            public Field run() {
                try {
                    final Field f = clazz.getDeclaredField(fieldName);
                    setAccessibility(f);
                    return f;
                } catch (final NoSuchFieldException ex) {
                    return null;
                }
            }
        };
    }



    /**
     * Create a privileged action to get all fields declared by the specified class.
     */
    public static PrivilegedAction<Field[]> getDeclaredFields(final Class<?> clazz) {
        return new PrivilegedAction<Field[]>() {
            public Field[] run() {
                final Field[] fields = clazz.getDeclaredFields();
                if (fields.length > 0)
                    AccessibleObject.setAccessible(fields, true);
                return fields;
            }
        };
    }



    /**
     * Create a privileged action to get all methods declared by the specified class.
     */
    public static PrivilegedAction<Method[]> getDeclaredMethods(final Class<?> clazz) {
      // XXX 2011-03-27 jw: Inconsistent behaviour.
      // doGetDeclaredFields() is setting fields accessible, but here we don't.
      return new PrivilegedAction<Method[]>() {
          public Method[] run() {
            return clazz.getDeclaredMethods();
        }
      };
    }

    /**
     * Create a privileged action to get the named method declared by the specified class
     * or by one of its ancestors.
     * The result of the action will be {@code null} if there is no such method.
     */
    public static PrivilegedAction<Method> getPublicMethod(final Class<?> clazz, final String methodName) {
      return new PrivilegedAction<Method>() {
          public Method run() {
              try {
                  return clazz.getMethod(methodName, (Class[]) null);
              } catch (final NoSuchMethodException ex) {
                  return null;
              }
          }
      };
    }

    private static void setAccessibility(Field field) {
      // FIXME 2011-03-27 jw:
      // - Why not simply call field.setAccessible(true)?
      // - Fields can not be abstract.
        if (!Modifier.isPublic(field.getModifiers()) || (
              Modifier.isPublic(field.getModifiers()) &&
                    Modifier.isAbstract(field.getModifiers()))) {
            field.setAccessible(true);
        }
    }

    static <T> T doPrivileged(final PrivilegedAction<T> action) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        } else {
            return action.run();
        }
    }

    private static final class GetContextClassLoader extends Object implements PrivilegedAction<ClassLoader> {

      static final GetContextClassLoader instance = new GetContextClassLoader();

      private GetContextClassLoader()
      {
        super();
      }

      public final ClassLoader run() {
          return Thread.currentThread().getContextClassLoader();
      }

    }

}
