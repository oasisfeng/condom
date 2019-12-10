package com.oasisfeng.condom;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * A simple service to mimic the real-world service to be wrapped by {@link com.oasisfeng.condom.CondomProcess CondomProcess}.
 *
 * Created by Oasis on 2017/10/2.
 */
public class TestService extends Service {

	interface Procedure { void run(final Context context); }

	static void invokeService(final IBinder binder, final Procedure procedure) {
		final Parcel data = Parcel.obtain(), reply = Parcel.obtain();
		final Class<? extends Procedure> clazz = procedure.getClass();
		data.writeString(clazz.getName());
		final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
		if (constructors.length < 1) throw new IllegalArgumentException("Invalid lambda class: " + clazz);
		final Field[] fields = clazz.getDeclaredFields();
		if (fields.length != constructors[0].getParameterTypes().length)
			throw new IllegalArgumentException("Lambda parameters mismatch: " + Arrays.deepToString(fields));

		final Class<?> enclosing_class = clazz.getEnclosingClass();
		for (final Field field : fields) {
			field.setAccessible(true);
			final Object value;
			try {
				value = field.get(procedure);
			} catch (final IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			// Enclosing class is always carried by anonymous inner class, but useless here.
			final Class<?> type = value.getClass();
			if (type == enclosing_class || type == Context.class || type == Application.class) data.writeValue(null);
			else try {
				data.writeValue(value);
			} catch (final RuntimeException e) {
				throw new RuntimeException("Invalid lambda parameter type: " + value.getClass().getCanonicalName(), e);
			}
		}

		try {
			binder.transact(0, data, reply, 0);
		} catch (final RemoteException e) {
			throw new RuntimeException(e);	// Avoid throwing RemoteException everywhere.
		}

		data.recycle();
		final Throwable e = (Throwable) reply.readValue(TestService.class.getClassLoader());
		if (e instanceof Error) throw (Error) e;
		if (e instanceof RuntimeException) throw (RuntimeException) e;
		if (e != null) throw new RuntimeException("Exception thrown by remote procedure", e);
		reply.recycle();
	}

	@Override public @Nullable IBinder onBind(final Intent intent) {
		return new Binder() {
			@Override protected boolean onTransact(final int code, final Parcel data, final Parcel reply, final int flags) throws RemoteException {
				try {
					final Class<?> clazz = Class.forName(data.readString());
					final Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
					constructor.setAccessible(true);
					final Class<?>[] parameter_types = constructor.getParameterTypes();
					final Object[] args = new Object[parameter_types.length];
					for (int i = 0; i < args.length; i++) {
						if (parameter_types[i] == Context.class) args[i] = TestService.this;
						else if (parameter_types[i] == Application.class) args[i] = getApplication();
						else args[i] = data.readValue(getClassLoader());
					}
					final Procedure procedure = (Procedure) constructor.newInstance(args);
					procedure.run(TestService.this);
					reply.writeValue(null);
				} catch (final Throwable t) {
					reply.writeValue(t);
				}
				return true;
			}
		};
	}
}
