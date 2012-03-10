package dloader.pagejob;

public interface ProgressReporter <ProgressType> {
	void report(String type, ProgressType report);
}
