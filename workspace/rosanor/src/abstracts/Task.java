package abstracts;

public abstract class Task {
	public abstract void post(Runnable r, String name);
	public abstract void kill();
	public abstract boolean killed();
}
