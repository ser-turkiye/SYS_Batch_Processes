package ser;

import com.ser.blueline.*;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.bpm.TaskStatus;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.*;

import static ser.Utils.loadTableRows;

public class BatchDeleteProcess extends UnifiedAgent {
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

            Utils.loadDirectory(Conf.BatchProcess.MainPath);

            IInformationObject[] list = this.getAllSubReviewProcess(helper);
            this.deleteSubReviewProcess(list);
        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }
    public void deleteSubReviewProcess(IInformationObject[] list) throws Exception {
        JSONObject dbks = new JSONObject();
        String uniqueId = UUID.randomUUID().toString();
        String mtpn = "DELETE_SUBREVIEW_PROCESS_LOG";
        String projectNo = "",  docNumber = "", revNumber = "", mainDocID = "", processID = "", taskID = "";
        int cnt = 0;

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
                List<ILink> mainAttachLinks = proi.getLoadedInformationObjectLinks().getLinks();
                for(ILink link : mainAttachLinks){
                    IInformationObject infoSource = link.getSourceInformationObject();
                    IInformationObject infoTarget = link.getTargetInformationObject();
                    log.info("test");
                }

                cnt++;
                dbks.put("DocNo" + cnt, (docNumber != null  ? docNumber : ""));
                dbks.put("RevNo" + cnt, (revNumber != null  ? revNumber : ""));
                dbks.put("ProcessTitle" + cnt, (proi != null ? proi.getDisplayName() : ""));
                dbks.put("ProcessID" + cnt, (proi != null ? processID : ""));
                dbks.put("TaskID" + cnt, (taskID != null ? taskID : ""));
                dbks.put("DocID" + cnt, (mainDocID != null ? mainDocID : ""));
                dbks.put("ProjectNo" + cnt, (projectNo != null  ? projectNo : ""));
                docs.add(processID);
                //getDocumentServer().deleteInformationObject(getSes(), proi);
            }

            //TimeUnit.SECONDS.sleep(30);
            log.info("UpdateEngDocument batch...sleeping (30 second)");
        }

        projects = Utils.getProjectWorkspaces(helper);
        IDocument mtpl = null;
        for (String prjn : projects.keySet()) {
            IInformationObject prjt = (IInformationObject) projects.get(prjn);
            IDocument dtpl = Utils.getTemplateDocument(prjt, mtpn);
            if (dtpl == null) {
                continue;
            }
            mtpl = dtpl;
            break;
        }

        String tplMailPath = Utils.exportDocument(mtpl, Conf.BatchProcess.MainPath, mtpn + "[" + uniqueId + "]");

        loadTableRows(tplMailPath, 0, "Task", 0, docs.size());

        String excelPath = Utils.saveToExcel(tplMailPath, 0,
                Conf.BatchProcess.MainPath + "/" + mtpn + "[" + uniqueId + "].xlsx", dbks
        );
        String htmlPath = Utils.convertExcelToHtml(excelPath,
                0,
                Conf.BatchProcess.MainPath + "/" + mtpn + "[" + uniqueId + "].html");
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
