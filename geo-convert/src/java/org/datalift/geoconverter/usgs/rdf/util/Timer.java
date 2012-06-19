package org.datalift.geoconverter.usgs.rdf.util;
/**
 * Timer for tracking program performance
 * @author Andrew Bulen
 */
public class Timer {
	/** start time and current time. */
	private long m_start, m_current;
	/** default constructor, initialize start time. */
	public Timer() {
		m_start = System.currentTimeMillis();
	}
	/** resets the start time. */
	public final void reset() {
		m_start = System.currentTimeMillis();
	}
	/** @return returns the time elapsed in Milliseconds. */
	public final long getElapsedMs() {
		return (System.currentTimeMillis() - m_start);
	}
	/** @return returns the time elapsed in Seconds */
	public final long getElapsedS() {
		return ((System.currentTimeMillis() - m_start) / 1000);
	}
	/** @return returns the time elapsed in Minutes */
	public final long getElapsedMin() {
		return ((System.currentTimeMillis() - m_start) / (1000 * 60));
	}
	/**
	 * prints the elapsed time between current and start time.
	 * breaks down into hours, minutes, and seconds
	 */
	public final void printElapsedTime() {
		m_current = System.currentTimeMillis();
		long elapsed = (m_current - m_start) / 1000;
		// check if longer than 1 minute
		if (elapsed > 60) {
			// check if longer than 1 hour
			if (elapsed > (60 * 60)) {
				// output days
				if (elapsed > (60 * 60 * 24)) {
					float days = (float) elapsed / (60f * 60f * 24f);
					System.out.println("Elapsed Time: " + days + " days");
				} else { // output hours
					float h = (float) elapsed / (60f * 60f);
					long hr = (long) h;
					float m = (h - hr) * 60;
					long min = (long) (m);
					float s = (m - min) * 60;
					long sec = (long) (s);
					System.out.println("Elapsed Time: " + hr + " hours "
						+ min + " minutes " + sec + " seconds");
				}
			} else { // output minutes
				float m = (float) elapsed / 60f;
				long min = (long) m;
				float s = (m - min) * 60;
				long sec = (long) (s);
				System.out.println("Elapsed Time: " + min + " minutes "
						+ sec + " seconds");
			}
		} else { // output seconds
			System.out.println("Elapsed Time: "
					+ elapsed + " seconds");
		}
	}
}
