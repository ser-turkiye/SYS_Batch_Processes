package ser;

import com.ser.blueline.IDocument;
import com.ser.blueline.IInformationObject;
import com.ser.blueline.ILink;
import com.ser.blueline.InformationObjectType;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.bpm.TaskStatus;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class DeleteEngDocuments extends UnifiedAgent {
    private Logger log = LogManager.getLogger();
    private ProcessHelper helper;
    List<String> others = new ArrayList<>();
    String decisionCode = "";
    @Override
    protected Object execute() {
        if (getBpm() == null)
            return resultError("Null BPM object");

        Utils.session = getSes();
        Utils.bpm = getBpm();
        Utils.server = Utils.session.getDocumentServer();
        try {
            helper = new ProcessHelper(getSes());
            log.info("-- DeleteEngDocuments Agent Started -----");
            IInformationObject[] list = this.getEmptyEngineeringDocs(helper);
            for(IInformationObject infoDoc : list){
                deleteDocument((IDocument) infoDoc);
            }
        } catch (Exception e) {
            log.error("DeleteEngDocuments Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        log.info("-- DeleteEngDocuments Agent Finished -----");
        return resultSuccess("Agent Finished Succesfully");
    }
    private void deleteDocument(IDocument mainDoc) throws Exception {
        String prjCode = mainDoc.getDescriptorValue(Conf.Descriptors.ProjectNo);
        String docCode = mainDoc.getDescriptorValue(Conf.Descriptors.DocNumber);
        String revCode = mainDoc.getDescriptorValue(Conf.Descriptors.Revision);
        log.info("Deleting Document :" + mainDoc.getID());
        try {
            if(!Objects.equals(mainDoc.getClassID(), Conf.ClassIDs.EngineeringCopy)) {
                List<ILink> mainAttachLinks = getEventTask().getProcessInstance().getLoadedInformationObjectLinks().getLinks();
                ILink[] links = getDocumentServer().getReferencedRelationships(getSes(), mainDoc, false, false);
                ILink[] links2 = getDocumentServer().getReferencingRelationships(getSes(), mainDoc.getID(), false);
                for (ILink link : links) {
                    IInformationObject xdoc = link.getTargetInformationObject();
                    String docInfo = xdoc.getDisplayName();
                    log.info("Delete link doc : " + xdoc.getID());
                    getDocumentServer().deleteInformationObject(getSes(), xdoc);
                    others.add(docInfo);
                    //Utils.server.removeRelationship(Utils.session, link);
                    log.info("deleted link doc");
                }
                for (ILink link2 : links2) {
                    IInformationObject xdoc = link2.getSourceInformationObject();
                    String docClassID = xdoc.getClassID();
                    InformationObjectType objType = xdoc.getInformationObjectType();
                    log.info("Delete usage object : " + xdoc.getID() + " /// type : " + objType);

                    if (Objects.equals(docClassID, Conf.ClassIDs.ProjectCard)) {
                        continue;
                    }
                    others.add(xdoc.getDisplayName());
                    getDocumentServer().deleteInformationObject(getSes(), xdoc);
                    log.info("deleted usage object");
                }
                IInformationObject[] infoEngCopyDocs = Utils.getEngineeringCopyDocuments(helper, prjCode, docCode, revCode);
                for (IInformationObject infoCopyDoc : infoEngCopyDocs) {
                    infoCopyDoc.setDescriptorValue("ObjectName", "DELETED");
                    infoCopyDoc.commit();
                }
            }
            else {
                ILink[] links2 = getDocumentServer().getReferencingRelationships(getSes(), mainDoc.getID(), false);
                for (ILink link2 : links2) {
                    IInformationObject xdoc = link2.getSourceInformationObject();
                    String docClassID = xdoc.getClassID();
                    InformationObjectType objType = xdoc.getInformationObjectType();
                    log.info("Delete usage object for copydoc : " + xdoc.getID() + " /// type : " + objType);
                    if (objType != InformationObjectType.PROCESS_INSTANCE) {
                        continue;
                    }
                    if (Objects.equals(docClassID, Conf.ClassIDs.ProjectCard)) {
                        continue;
                    }
                    if (Objects.equals(docClassID, Conf.ClassIDs.DeleteProcess)) {
                        continue;
                    }
                    others.add(xdoc.getDisplayName());
                    getDocumentServer().deleteInformationObject(getSes(), xdoc);
                    log.info("deleted usage object for copydoc ");
                }
            }
            getDocumentServer().deleteInformationObject(getSes(),mainDoc);
            log.info("Deleted Document");
        } catch (Exception e) {
            throw new Exception("Exeption Caught..deleteDocument: " + e);
        }
    }
    public IInformationObject[] getEmptyEngineeringDocs(ProcessHelper helper) {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.EngineeringDocument).append("'")
                .append(" AND ")
                .append("CCMPRJCARD_CODE").append(" IS NOT NULL");
                //.append(" AND ")
                //.append("CCMPRJDOCNUMBER").append(" IS NULL")
                //.append(" AND ")
                //.append("CCMPRJDOCFILENAME").append(" IS NULL");
        String whereClause = builder.toString();
        log.info("Where Clause: " + whereClause);
        IInformationObject[] list = helper.createQuery(new String[]{Conf.Databases.EngineeringDocument}, whereClause, "", 0, false);
        return list;
    }
}
