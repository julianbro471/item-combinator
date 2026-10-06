package com.combinator.ability;

import com.combinator.ItemCombinator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntConsumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * A tiny scheduler: run something a number of game ticks from now (20 ticks = 1 second).
 * Used for bomb fuses, black holes, meteor showers and everything else that takes time.
 * Scheduled work is forgotten when the world is closed.
 */
public final class Tasks {
	private record Task(long due, Runnable action) {
	}

	private static final List<Task> QUEUE = new ArrayList<>();
	private static long now = 0;

	private Tasks() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> QUEUE.clear());
	}

	/** Runs the action once, the given number of ticks from now. */
	public static void later(int ticks, Runnable action) {
		QUEUE.add(new Task(now + Math.max(1, ticks), action));
	}

	/** Runs the step the given number of times, with the given number of ticks between runs. The step gets 0, 1, 2, ... */
	public static void repeat(int times, int interval, IntConsumer step) {
		for (int i = 0; i < times; i++) {
			int index = i;
			later(1 + i * interval, () -> step.accept(index));
		}
	}

	private static void tick() {
		now++;
		if (QUEUE.isEmpty()) {
			return;
		}
		List<Task> due = null;
		for (Iterator<Task> it = QUEUE.iterator(); it.hasNext(); ) {
			Task task = it.next();
			if (task.due() <= now) {
				if (due == null) {
					due = new ArrayList<>();
				}
				due.add(task);
				it.remove();
			}
		}
		if (due == null) {
			return;
		}
		for (Task task : due) {
			try {
				task.action().run();
			} catch (Throwable t) {
				ItemCombinator.LOGGER.error("Scheduled ability step failed", t);
			}
		}
	}
}
