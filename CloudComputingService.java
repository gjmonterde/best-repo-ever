package track;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CloudComputingService {
    private DecimalFormat doubleFormat = new DecimalFormat("###");

    private CloudComputingServicePlan myCCSPlan;
    private Map<LocalDateTime, Double> instances = new HashMap<LocalDateTime, Double>();
    private Map<Character, Double> consumption = new HashMap<Character, Double>();
    private double peakStorageSpace = 0;
    private double uptimeSpent = 0;
    private LocalDateTime shutdownDateTime = null;
    private LocalDateTime currentDateTime = null;
    private double currentTotalUptime = 0.0;
    private List<String> output = new ArrayList<String>();

    public CloudComputingService(String[] getStdin){
        // Set initial plan to Free Tier
        myCCSPlan = CloudComputingServicePlan.getPlan();
        consumption.put(CloudComputingServicePlan.TRN_AMT_LMT_ABBRE, 0.0);
        consumption.put(CloudComputingServicePlan.STR_SPC_LMT_ABBRE, 0.0);
        consumption.put(CloudComputingServicePlan.USAGE_FEE_LMT_ABBRE, 0.0);

        // Iterate every request
        for(String request : getStdin){
            String[] requestInfo = request.split(" ");
            // The request type: UPLOAD, DOWNLOAD, DELETE, LAUNCH, STOP, CALC, UPGRADE, CHANGE
            String type = requestInfo[0];
            String sentDate = !type.equals("CALC") ? requestInfo[1] : "";
            String sentTime = !type.equals("CALC") ? requestInfo[2] : "";
            // The date and time the request is sent.
            LocalDateTime sentDateTime = !sentDate.isEmpty() && !sentTime.isEmpty() ? LocalDateTime.parse(sentDate +"T" + sentTime) : null;
            currentDateTime = sentDateTime != null ? sentDateTime : currentDateTime;
            Double requestSize = type.equals("CALC") ? null : 
                                type.equals("STOP") ? Double.parseDouble(requestInfo[5]) : 
                                type.equals("CHANGE") ? Double.parseDouble(requestInfo[4]) :
                                Double.parseDouble(requestInfo[3]);

            // Status is Usage Fee Overrun State
            if(shutdownDateTime != null && sentDateTime != null && sentDateTime.isAfter(shutdownDateTime)
                 && !type.equals("UPGRADE") && !type.equals("CHANGE")){
                output.add(String.format("%s: please increase usage fee limit", type));
                continue;
            }

            // Proceed if request is UPLOAD
            String log = this.getUpload(type, sentDateTime, requestSize);
            if(!log.isEmpty()){
                output.add(log);
                continue;
            }

            // Proceed if request is DOWNLOAD
            log = this.getDownload(type, sentDateTime, requestSize);
            if(!log.isEmpty()){
                output.add(log);
                continue;
            }

            // Proceed if request is DELETE
            log = this.getDelete(type, requestSize);
            if(!log.isEmpty()){
                output.add(log);
                continue;
            }

            // Proceed if request is LAUNCH
            log = this.getLaunch(type, sentDateTime, requestSize);
            if(!log.isEmpty()){
                output.add(log);
                continue;
            }

            // Proceed if request is STOP
            String launchDate = type.equals("STOP") ? requestInfo[3] : "";
            String launchTime = type.equals("STOP") ? requestInfo[4] : "";
            LocalDateTime launchDateTime = !launchDate.isEmpty() && !launchTime.isEmpty() ? LocalDateTime.parse(launchDate + "T" + launchTime) : null;
            
            log = this.getStop(type, sentDateTime, launchDateTime, requestSize);
            if(!log.isEmpty()){
                output.add(log);
                continue;
            }

            // Proceed if request is CALC
            log = this.getCalc(type);
            if(!log.isEmpty()){
                output.add(log);
                continue;
            }

            // Proceed if request is UPGRADE
            log = this.getUpgrade(type, sentDateTime, requestSize);
            if(!log.isEmpty()){
                output.add(log);
                continue;
            }

            // Proceed if request is CHANGE
            String abbreviation = !type.equals("CALC") ? requestInfo[3] : "";
            
            log = this.getChange(type, sentDateTime, abbreviation, requestSize);
            if(!log.isEmpty()){
                output.add(log);
                continue;
            }
        }
    }

    
    /** 
     * Return the overall output to App.java
     * @return String[]
     */
    public String[] getOutput(){
        return output.toArray(new String[output.size()]);
    }

    
    /** 
     * Check if the current request exceeds the user's limit
     * @param tempTransferAmt
     * @param tempStorageSpace
     * @param tempUsageFee
     * @return String
     */
    private String getAbbreviation(double tempTransferAmt, double tempStorageSpace, double tempUsageFee){
        if(tempTransferAmt > myCCSPlan.getTransferAmountLimit())
            return String.valueOf(CloudComputingServicePlan.TRN_AMT_LMT_ABBRE);
        if(tempStorageSpace > myCCSPlan.getStorageSpaceLimit())
            return String.valueOf(CloudComputingServicePlan.STR_SPC_LMT_ABBRE);
        if( tempUsageFee > myCCSPlan.getUsageFeeLimit())
            return String.valueOf(CloudComputingServicePlan.USAGE_FEE_LMT_ABBRE);

        return "";
    }

    
    /** 
     * @param type
     * @param sentDateTime
     * @param fileSize 
     * @return String
     */
    private String getUpload(String type, LocalDateTime sentDateTime, Double fileSize){
        if(!type.equals("UPLOAD") || fileSize == null)
            return "";

        // Compute the uptime of servers up to this date
        this.uptimeSpent = computeUpTime(sentDateTime);

        double tempTransferAmt = consumption.get(CloudComputingServicePlan.TRN_AMT_LMT_ABBRE) + fileSize;
        double tempStorageSpace = consumption.get(CloudComputingServicePlan.STR_SPC_LMT_ABBRE) + fileSize;

        double transferUsageFee = (CloudComputingServicePlan.TRANSFER_FEE * (tempTransferAmt - CloudComputingServicePlan.TRN_AMT_FREE_TIER));
        double storageUsageFee = (CloudComputingServicePlan.STORAGE_FEE * (tempStorageSpace - CloudComputingServicePlan.STR_SPC_FREE_TIER));
        double tempUsageFee = consumption.get(CloudComputingServicePlan.USAGE_FEE_LMT_ABBRE) + (transferUsageFee > 0 ? transferUsageFee : 0) + (storageUsageFee > 0 ? storageUsageFee : 0);

        // Check if this request will exceed the user's plan limits
        String abbreviation = this.getAbbreviation(tempTransferAmt, tempStorageSpace, tempUsageFee);
        if(!abbreviation.isEmpty())
            return String.format("%s: %s", type, abbreviation);
        
        // Add the transfer amount to user's consumed transfer amount, storage space and computed usage fee
        consumption.put(CloudComputingServicePlan.TRN_AMT_LMT_ABBRE, tempTransferAmt);
        consumption.put(CloudComputingServicePlan.STR_SPC_LMT_ABBRE, tempStorageSpace);
        peakStorageSpace = tempStorageSpace > peakStorageSpace ? tempStorageSpace : peakStorageSpace;
        consumption.put(CloudComputingServicePlan.USAGE_FEE_LMT_ABBRE, tempUsageFee);

        // Get the expected shutdown date of running servers
        double totalInstances = this.instances.values().stream().mapToDouble(Double::doubleValue).sum();
        this.shutdownDateTime = getShutdownDateTime(sentDateTime, totalInstances, this.uptimeSpent);

        return String.format("%s: %s %s %s", 
            type, 
            this.doubleFormat.format(tempTransferAmt), 
            this.doubleFormat.format(tempStorageSpace), 
            this.shutdownDateTime == null? "-" : this.shutdownDateTime);
    }

    
    /** 
     * @param type
     * @param sentDateTime
     * @param fileSize
     * @return String
     */
    private String getDownload(String type, LocalDateTime sentDateTime, Double fileSize){
        if(!type.equals("DOWNLOAD") || fileSize == null)
            return "";

        // Compute the uptime of servers up to this date
        this.uptimeSpent = computeUpTime(sentDateTime);

        double tempTransferAmt = consumption.get(CloudComputingServicePlan.TRN_AMT_LMT_ABBRE) + fileSize;
        double storageSpace = consumption.get(CloudComputingServicePlan.STR_SPC_LMT_ABBRE);
        double transferUsageFee = (CloudComputingServicePlan.TRANSFER_FEE * (tempTransferAmt - CloudComputingServicePlan.TRN_AMT_FREE_TIER));
        double tempUsageFee = consumption.get(CloudComputingServicePlan.USAGE_FEE_LMT_ABBRE) + (transferUsageFee > 0 ? transferUsageFee : 0);
                
        // Check if this request will exceed the user's plan limits
        String abbreviation = this.getAbbreviation(tempTransferAmt, storageSpace, tempUsageFee);
        if(!abbreviation.isEmpty())
            return String.format("%s: %s", type, abbreviation);
        
        if(fileSize > storageSpace)
            return String.format("%s: %s", type, "no such files");
        
        // Add the transfer amount to user's consumed transfer amount and computed usage fee
        consumption.put(CloudComputingServicePlan.TRN_AMT_LMT_ABBRE, tempTransferAmt);
        consumption.put(CloudComputingServicePlan.USAGE_FEE_LMT_ABBRE, tempUsageFee);

        // Get the expected shutdown date of running servers
        double totalInstances = this.instances.values().stream().mapToDouble(Double::doubleValue).sum();
        this.shutdownDateTime = getShutdownDateTime(sentDateTime, totalInstances, this.uptimeSpent);
        
        return String.format("%s: %s %s", 
            type, 
            this.doubleFormat.format(tempTransferAmt), 
            this.shutdownDateTime == null? "-" : this.shutdownDateTime);
    }

    
    /** 
     * @param type
     * @param fileSize
     * @return String
     */
    private String getDelete(String type, Double fileSize){
        if(!type.equals("DELETE") || fileSize == null)
            return "";

        if(fileSize > consumption.get(CloudComputingServicePlan.STR_SPC_LMT_ABBRE))
            return String.format("%s: %s", type, "no such files");
        
        // Add the user's consumed storage space
        consumption.put(CloudComputingServicePlan.STR_SPC_LMT_ABBRE, consumption.get(CloudComputingServicePlan.STR_SPC_LMT_ABBRE) - fileSize);
        
        return String.format("%s: %s %s ", 
            type, 
            this.doubleFormat.format(consumption.get(CloudComputingServicePlan.STR_SPC_LMT_ABBRE)), 
            this.shutdownDateTime == null? "-" : this.shutdownDateTime);
    }

    
    /** 
     * @param type
     * @param sentDateTime
     * @param instances
     * @return String
     */
    private String getLaunch(String type, LocalDateTime sentDateTime, Double instances){
        if(!type.equals("LAUNCH") || sentDateTime == null || instances == null)
            return "";

        // Compute the uptime of servers up to this date
        this.uptimeSpent = computeUpTime(sentDateTime);

        // Add the request to running servers
        this.instances.put(sentDateTime, instances);
        
        // Get the expected shutdown date of running servers
        double totalInstances = this.instances.values().stream().mapToDouble(Double::doubleValue).sum();
        this.shutdownDateTime = getShutdownDateTime(sentDateTime, totalInstances, this.uptimeSpent);
        
        // Check if the shutdown date is next month
        boolean isNextMonth = checkIfNextMonth(this.currentDateTime, shutdownDateTime);

        return String.format("%s: %s %s ", 
            type, 
            this.doubleFormat.format(totalInstances),
            isNextMonth ? "-" : this.shutdownDateTime.toString().replace("T", " "));
    }

    
    /** 
     * @param type
     * @param sentDateTime
     * @param launchDateTime
     * @param fileSize
     * @return String
     */
    private String getStop(String type, LocalDateTime sentDateTime, LocalDateTime launchDateTime, Double fileSize){
        if(!type.equals("STOP") || sentDateTime == null || launchDateTime == null || fileSize == null)
            return "";

        if(!this.instances.containsKey(launchDateTime) || this.instances.get(launchDateTime) < fileSize)
            return String.format("%s: %s", type, "please correctly specify the instances");
        

        this.uptimeSpent = computeUpTime(sentDateTime);

        // Deduct the request to running servers
        // If the running server for the specified instance is 0, remove the instance.
        this.instances.put(launchDateTime, this.instances.get(launchDateTime) - fileSize);
        this.instances.remove(launchDateTime, 0);

        // Get the expected shutdown date of running servers
        double totalInstances = this.instances.values().stream().mapToDouble(Double::doubleValue).sum();
        this.shutdownDateTime = getShutdownDateTime(sentDateTime, totalInstances, this.uptimeSpent);
        
        return String.format("%s: %s %s ", 
            type, 
            this.doubleFormat.format(totalInstances),
            this.shutdownDateTime.toString().replace("T", " "));
    }

    
    /** 
     * @param type
     * @return String
     */
    private String getCalc(String type){
        if(!type.equals("CALC"))
            return "";

        // Check if the shutdown date is next month
        boolean isNextMonth = checkIfNextMonth(currentDateTime, shutdownDateTime);
        
        double usageFee = 0.0;
        // Compute the overall usage fee including the running servers
        if(isNextMonth){
            LocalDateTime endOfMonth = currentDateTime.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).minusMinutes(1);
            usageFee = computeUsageFee(endOfMonth) + consumption.get(CloudComputingServicePlan.USAGE_FEE_LMT_ABBRE);
        }else if (shutdownDateTime != null){
            usageFee = computeUsageFee(shutdownDateTime.minusMinutes(1)) + consumption.get(CloudComputingServicePlan.USAGE_FEE_LMT_ABBRE);
            this.instances.clear();
        }else{
            usageFee = consumption.get(CloudComputingServicePlan.USAGE_FEE_LMT_ABBRE);
        }

        // add this log to output
        String outputCalc = String.format("%s: %s %s ", 
            type, 
            this.doubleFormat.format(usageFee),
            isNextMonth ? this.shutdownDateTime.toString().replace("T", " ") : "-");
        
        // Reset the transfer and usage fee consumption, peak storage space, shutdown date and current total uptime
        consumption.put(CloudComputingServicePlan.TRN_AMT_LMT_ABBRE, 0.0);
        consumption.put(CloudComputingServicePlan.USAGE_FEE_LMT_ABBRE, 0.0);
        peakStorageSpace = consumption.get(CloudComputingServicePlan.STR_SPC_LMT_ABBRE);
        this.shutdownDateTime = null;
        this.currentTotalUptime = 0.0;
        // set the current DateTime to next date in preparation for new requests next month
        currentDateTime = currentDateTime.plusMonths(1);

        return outputCalc;
    }

    
    /** 
     * @param type
     * @param sentDateTime
     * @param maxLimitSize
     * @return String
     */
    private String getUpgrade(String type, LocalDateTime sentDateTime, Double maxLimitSize){
        if(!type.equals("UPGRADE")  || sentDateTime == null || maxLimitSize == null)
            return "";
        
        if(myCCSPlan.isPaidUser() && myCCSPlan.getUsageFeeLimit() > maxLimitSize)
            return String.format("%s: %s ", type, "invalid value");

        
        if(myCCSPlan.isPaidUser() && myCCSPlan.getUsageFeeLimit() <= maxLimitSize){
            myCCSPlan.upgradePlan(maxLimitSize);
            return String.format("%s: %s ", type, "accepted");
        } 

        // Get the uptime if running servers up to this date
        uptimeSpent = computeUpTime(sentDateTime);
        
        myCCSPlan.newUpgradePlan(maxLimitSize);

        // Get the expected shutdown date of running servers after upgrading the plan
        double totalInstances = this.instances.values().stream().mapToDouble(Double::doubleValue).sum();
        this.shutdownDateTime = getShutdownDateTime(sentDateTime, totalInstances, this.uptimeSpent);

        return String.format("%s: %s", 
            type, 
            this.shutdownDateTime == null ? "-" : this.shutdownDateTime.toString().replace("T", " "));
    }

    
    /** 
     * @param type
     * @param sentDateTime
     * @param abbreviation
     * @param limit
     * @return String
     */
    private String getChange(String type, LocalDateTime sentDateTime, String abbreviation, Double limit){
        if(!type.equals("CHANGE")  || sentDateTime == null || abbreviation.isEmpty() || limit == null)
            return "";
        
        if(!myCCSPlan.isPaidUser())
            return String.format("%s: %s ", type, "free plan");

        // limit is between minimum and maximum value of that limit inclusive
        // current transfer amount, storage space, and usage fee >=  limit
        if((limit >= myCCSPlan.getMinimumLimits(abbreviation.charAt(0)) && limit <= myCCSPlan.getMaximumLimits(abbreviation.charAt(0)))
            && limit >= consumption.get(abbreviation.charAt(0))){

            myCCSPlan.setLimit(abbreviation.charAt(0), limit);

            // If shutdown is before the request date, get the new shutdown date
            if(this.shutdownDateTime != null && sentDateTime.isBefore(shutdownDateTime)){
                uptimeSpent = computeUpTime(sentDateTime);
                double totalInstances = this.instances.values().stream().mapToDouble(Double::doubleValue).sum();
                this.shutdownDateTime = getShutdownDateTime(sentDateTime, totalInstances, uptimeSpent);
            }else if(this.shutdownDateTime != null){
                // Else If shutdown is equal or after the request date,
                // And shudown date is not null, get the current total uptime from base on the shutdown date
                this.currentTotalUptime = computeUpTime(this.shutdownDateTime);
                // remove running servers since servers already shutdown at the time of request
                this.instances.clear();
                this.shutdownDateTime = null;
            }
            
            return String.format("%s: %s", 
                type, 
                this.shutdownDateTime == null ? "-" : this.shutdownDateTime.toString().replace("T", " "));
        }

        return String.format("%s: %s", type, "invalid value");
    }

    /** ******************************
     * ***** HELPER METHODS **********
     * *******************************
     */

    /** 
     * Compute the current total uptime until the given date
     * @param sentDateTime
     * @return Double
     */
    private Double computeUpTime(LocalDateTime sentDateTime){

        if(sentDateTime == null)
            return 0.0;

        double uptime = 0.0;

        for (Map.Entry<LocalDateTime, Double> entry : this.instances.entrySet()) {
            boolean isNextMonth = checkIfNextMonth(entry.getKey(), sentDateTime);
            Duration duration = Duration.between(isNextMonth ? sentDateTime.withDayOfMonth(1).withHour(0).withMinute(0).minusMinutes(1) : entry.getKey() , sentDateTime);
            uptime += (duration.toMinutes() * entry.getValue());
        }

        return uptime;
        
    }

    
    /** 
     * @param sentDateTime
     * @param totalInstances
     * @param spentUptime
     * @return LocalDateTime
     */
    private LocalDateTime getShutdownDateTime(LocalDateTime sentDateTime, double totalInstances, double spentUptime){

        if(this.instances.isEmpty())
            return null;

        double minutesToShutdown = ((myCCSPlan.getUsageFeeLimit() - consumption.get(CloudComputingServicePlan.USAGE_FEE_LMT_ABBRE)) // deduct usage fee limit to current usafe fee
            / CloudComputingServicePlan.INSTANCE_FEE // convert usage fee limit yen to hours
            * 60 // convert hours to minutes
            + CloudComputingServicePlan.VIRTUAL_SERVER_FREE_TIER // add the free tier of virtual server
            - this.currentTotalUptime // the total uptime from the date of last auto-shutdown 
            - spentUptime) // deduct the uptime spent
            / totalInstances // divide with the total virtual servers
            ;
        
        return sentDateTime.plusMinutes(Double.valueOf(minutesToShutdown).longValue()+1);
        
    }

    
    /** 
     * Compute the usage fee of running virtual servers
     * This method is only executed during CALC request
     * @param currenDateTime
     * @return double
     */
    private double computeUsageFee(LocalDateTime currenDateTime){
        doubleFormat.setRoundingMode(RoundingMode.DOWN);

        uptimeSpent = this.currentTotalUptime + computeUpTime(currenDateTime) - CloudComputingServicePlan.VIRTUAL_SERVER_FREE_TIER; // in minutes
        String uptimeInHours = doubleFormat.format((uptimeSpent > 0 ? uptimeSpent : 0) / 60);
        
        return (CloudComputingServicePlan.INSTANCE_FEE * Double.valueOf(uptimeInHours));
    }

    
    /** 
     * @param from
     * @param to
     * @return boolean
     */
    private boolean checkIfNextMonth(LocalDateTime from, LocalDateTime to){
        return from == null || to == null ? false : from.getYear() < to.getYear() || (from.getYear() == to.getYear() && from.getMonthValue() < to.getMonthValue());
    }
    
}
