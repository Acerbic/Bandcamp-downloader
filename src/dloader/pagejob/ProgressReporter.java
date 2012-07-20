package dloader.pagejob;

/**
 * Very simple interface to unify reporting and messaging between orthogonal modules.
 * @author Acerbic
 *
 */
public interface ProgressReporter  {
	void report(String type, int report);
}
