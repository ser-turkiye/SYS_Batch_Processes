package ser;

import com.ser.blueline.IDocument;
import com.ser.blueline.IInformationObject;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.bpm.TaskStatus;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class BatchDeleteTmpFiles extends UnifiedAgent {
    private Logger log = LogManager.getLogger();
    private ProcessHelper helper;
    JSONObject projects = new JSONObject();
    List<String> docs = new ArrayList<>();
    @Override
    protected Object execute() {
        try {
            if (getBpm() == null)
                return resultError("Null BPM object");
            Utils.session = getSes();
            Utils.bpm = getBpm();
            Utils.server = Utils.session.getDocumentServer();
            this.helper = new ProcessHelper(Utils.session);

            List<String> exeptionList = new ArrayList<>();
            exeptionList.add("C:\\tmp2\\bulk\\batchprocess");

            String dirWay = Conf.BatchProcess.TmpPath;
            int daysBack = 3;
            long currentTime = System.currentTimeMillis();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(currentTime);
            Date currentStrTime = c.getTime();
            long purgeTime = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000);
            c.setTimeInMillis(purgeTime);
            Date purgeStrTime = c.getTime();
            File directory = new File(dirWay);
            if(directory.exists()){
                File[] listFiles = directory.listFiles();
                assert listFiles != null;
                for(File listFile : listFiles) {
                    if(exeptionList.contains(listFile.getPath())){
                        continue;
                    }
                    if(listFile.isDirectory()){
                        File directory2 = new File(listFile.getPath());
                        File[] listFiles2 = directory2.listFiles();
                        assert listFiles2 != null;
                        for(File listFile2 : listFiles2) {
                            File directory3 = new File(listFile2.getPath());
                            c.setTimeInMillis(listFile2.lastModified());
                            Date modifiedStrTime = c.getTime();
                            if(listFile2.isDirectory()){
                                if (listFile2.lastModified() < purgeTime) {
                                    FileUtils.deleteDirectory(directory3);
                                }
                            }else {
                                if (listFile2.lastModified() < purgeTime) {
                                    if (!listFile2.delete()) {
                                        System.err.println("Unable to delete file: " + listFile2);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                log.warn("Files were not deleted, directory " + dirWay + " does'nt exist!");
            }

        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }
    public void deleteFiles(String path){

    }
}
