package ser;

import com.ser.blueline.IDocument;
import com.ser.blueline.IInformationObject;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.bpm.TaskStatus;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ser.Utils.loadTableRows;

public class DeleteSubReviewProcessSch extends UnifiedAgent {
    private Logger log = LogManager.getLogger();
    private ProcessHelper helper;
    JSONObject projects = new JSONObject();
    List<String> docs = new ArrayList<>();
    int count = 0;
    @Override
    protected Object execute() {
        try {
            if (getBpm() == null)
                return resultError("Null BPM object");
            Utils.session = getSes();
            Utils.bpm = getBpm();
            Utils.server = Utils.session.getDocumentServer();
            this.helper = new ProcessHelper(Utils.session);
            log.info("DeleteDubReviewProcess batch...start");
            IInformationObject[] list = this.getAllSubReviewProcess(helper);
            this.deleteSubReviewProcess(list);
            log.info("DeleteDubReviewProcess batch...finish");
        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }
    public void deleteSubReviewProcess(IInformationObject[] list) throws Exception {
        String projectNo = "",  docNumber = "", revNumber = "", mainDocID = "", processID = "", taskID = "";
        for(IInformationObject infoDoc : list){
            ITask task = (ITask) infoDoc;
            TaskStatus taskStatus = task.getStatus();
            IProcessInstance proi = task.getProcessInstance();

            processID = proi.getID();
            taskID = task.getID();
            mainDocID = task.getDescriptorValue(Conf.Descriptors.MainDocumentID);
            projectNo = task.getDescriptorValue(Conf.Descriptors.ProjectNo);
            docNumber = task.getDescriptorValue(Conf.Descriptors.DocNumber);
            revNumber = task.getDescriptorValue(Conf.Descriptors.Revision);

            if(docs.contains(processID)){continue;}

            IDocument mainDoc = Utils.server.getDocument4ID(mainDocID, Utils.session);
            if(mainDoc == null){
                log.info("DELETE SUBREVIEW START FOR PRCS ID:" + processID);
                log.info("DELETE SUBREVIEW REF MAIN DOC ID:" + mainDocID);
                log.info("DELETE SUBREVIEW DOC INFO:[" + projectNo + "] // [" + docNumber + "] // [" + revNumber + "]");
                getDocumentServer().deleteInformationObject(getSes(), proi);
                log.info("DELETED SUBREVIEW PRCS ID:" + processID);
                count++;
            }
        }
        log.info("DELETED SUBREVIEW COUNT:[" + count + "]");
    }
    public IInformationObject[] getAllSubReviewProcess(ProcessHelper helper) {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.SubProcess).append("'");
        String whereClause = builder.toString();
        log.info("Where Clause: " + whereClause);
        IInformationObject[] list = helper.createQuery(new String[]{Conf.Databases.Process}, whereClause, "", 0, false);
        return list;
    }
    public IInformationObject[] getSubReviewProcessByDocNumber(ProcessHelper helper, String docNumber) {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.SubProcess).append("'");
        String whereClause = builder.toString();
        log.info("Where Clause: " + whereClause);
        IInformationObject[] list = helper.createQuery(new String[]{Conf.Databases.Process}, whereClause, "", 0, false);
        return list;
    }
    public IInformationObject[] getEngineeringDocument(ProcessHelper helper, String prjn, String docNumber, String docRev) {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.EngineeringCopy).append("'")
                .append(" AND ")
                .append(Conf.DescriptorLiterals.PrjCardCode).append(" = '").append(prjn).append("'")
                .append(" AND ")
                .append(Conf.DescriptorLiterals.PrjDocNumber).append(" = '").append(docNumber).append("'")
                .append(" AND ")
                .append(Conf.DescriptorLiterals.PrjDocRev).append(" = '").append(docRev).append("'");
        //builder.append(" AND WFL_TASK_STATUS IN (2,4,16)");
        String whereClause = builder.toString();
        log.info("Where Clause: " + whereClause);
        IInformationObject[] list = helper.createQuery(new String[]{Conf.Databases.EngineeringCopy}, whereClause, "", 0, false);
        //if(list.length < 1) {return null;}
        return list;
    }
}
