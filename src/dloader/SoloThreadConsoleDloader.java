package dloader;


import dloader.page.AbstractPage;

/**
 * Console application that downloads everything in a single thread.
 * (well, technically not, since ThreadPool executor is residing in a separate thread.)
 * 
 * @author Acerbic
 *
 */
public class SoloThreadConsoleDloader {

	static public class STCD_JobMaster extends JobMaster {

		public STCD_JobMaster(JobType whatToDo, AbstractPage rootPage) {
			super(whatToDo, rootPage, 1);
		}
		
		@Override
		public void report(AbstractPage page, String type, long report) {
			AbstractPage t;
			switch (type) {
			case "download job started":
				t = page.getParent(); while (t != null) {System.out.print("    "); t = t.getParent();};
				System.out.print("Updating \""+page.toString() + "\"... ");
				break;
			case "downloaded":
			case "up to date":
			case "download failed":
				System.out.println(type); break;


			case "saving started":
				t = page.getParent(); while (t != null) {System.out.print("    "); t = t.getParent();};
				System.out.print("Saving \""+page.toString() + "\"... ");
				break;
			case "save skipped":
				System.out.println("skipped"); break;
			case "saving caused exception":
				System.out.println(" ERROR!! "); break;
			case "file updated":
				System.out.println("tags updated"); break;
			case "file downloaded":
				System.out.println("downloaded"); break;
			case "file size":
				System.out.printf("(%d Kb)", report/1024); break;
			case "downloaded bytes":
				System.out.print("*");break;
			case "cover image downloaded":
				System.out.println(type); break;
				
			case "checking cache":
				t = page.getParent(); while (t != null) {System.out.print("    "); t = t.getParent();};
				System.out.print("Reading from cache \""+page.toString() + "\"... ");
				break;
			case "read from cache":
				System.out.println("OK"); break;
			case "read cache failed":
				System.out.println("failed"); break;
			case "cache reading failed, submitting download job":
				System.out.println("failed, scheduled for DOWNLOAD"); break;
				
			case "download finished": break;
			default:
				System.out.println("Unknown report:" + page + " " + type + report);
			}
		}
	}
	
	
	public void getThingsMoving() {
		
		System.out.println("-------Starting. Reading cached page data-------");
		
		AbstractPage p = AbstractPage.bakeAPage(null, Main.baseURL, Main.saveTo, null);
		
		//1 read cache;
		STCD_JobMaster jm;
//		jm = new STCD_JobMaster(JobMaster.JobType.READCACHEPAGES, p);
//		jm.goGoGo();
		
		System.out.println("-------Updating page data------");
		//2 update page data
		jm = new STCD_JobMaster(JobMaster.JobType.UPDATEPAGES, p);
		jm.goGoGo();

		System.out.println("-------Saving files-------");
		//3 save page data
		jm = new STCD_JobMaster(JobMaster.JobType.SAVEDATA, p);
		jm.goGoGo();
	}
}
