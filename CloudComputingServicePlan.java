package track;

import java.util.HashMap;
import java.util.Map;

public class CloudComputingServicePlan {
    private static CloudComputingServicePlan ccsLimits = new CloudComputingServicePlan();
    public static final char TRN_AMT_LMT_ABBRE = 't';
    public static final char STR_SPC_LMT_ABBRE = 's';
    public static final char USAGE_FEE_LMT_ABBRE = 'u';
    public static final double TRN_AMT_FREE_TIER = 10000000000.0;
    public static final double STR_SPC_FREE_TIER = 20000000000.0;
    public static final double USAGE_FEE_FREE_TIER = 0;
    public static final double VIRTUAL_SERVER_FREE_TIER = 6000;
    public static final double TRANSFER_FEE = Double.valueOf(1) / Double.valueOf(100000000);
    public static final double STORAGE_FEE = Double.valueOf(1) / Double.valueOf(1000000000);
    public static final double INSTANCE_FEE = 100;

    private boolean isPaidPlanUser = false;
    private Map<Character,Double> limits ;
    private Map<Character,Double> minimumLimits ;
    private Map<Character,Double> maximumLimits ;

    private CloudComputingServicePlan(){
        this.limits = new HashMap<Character,Double>();
        this.limits.put(TRN_AMT_LMT_ABBRE, TRN_AMT_FREE_TIER);
        this.limits.put(STR_SPC_LMT_ABBRE, STR_SPC_FREE_TIER);
        this.limits.put(USAGE_FEE_LMT_ABBRE, USAGE_FEE_FREE_TIER);

        this.minimumLimits = new HashMap<Character,Double>();
        this.minimumLimits.put(TRN_AMT_LMT_ABBRE, 1.0);
        this.minimumLimits.put(STR_SPC_LMT_ABBRE, 1.0);
        this.minimumLimits.put(USAGE_FEE_LMT_ABBRE, 100.0);

        this.maximumLimits = new HashMap<Character,Double>();
        this.maximumLimits.put(TRN_AMT_LMT_ABBRE, 100000000000000.0);
        this.maximumLimits.put(STR_SPC_LMT_ABBRE, 100000000000000.0);
        this.maximumLimits.put(USAGE_FEE_LMT_ABBRE, 100.0);
    }

    
    /** 
     * Get the current instance of CloudComputingServicePlan
     * @return CloudComputingServicePlan
     */
    public static CloudComputingServicePlan getPlan(){
        return ccsLimits;
    }

    
    /** 
     * Check if user has free plan or paid plan
     * @return boolean
     */
    public boolean isPaidUser(){
        return this.isPaidPlanUser;
    }

    
    /** 
     * Upgrade the user to paid plan
     * @param maxLimitSize
     */
    public void newUpgradePlan(double maxLimitSize){
        this.limits.put(TRN_AMT_LMT_ABBRE, 100000000000.0);
        this.limits.put(STR_SPC_LMT_ABBRE, 100000000000.0);
        this.limits.put(USAGE_FEE_LMT_ABBRE, 10000.0);
        this.maximumLimits.put(USAGE_FEE_LMT_ABBRE, maxLimitSize);
        this.isPaidPlanUser = true;
    }

    /** 
     * Upgrade the user to paid plan
     * @param maxLimitSize
     */
    public void upgradePlan(double maxLimitSize){
        this.maximumLimits.put(USAGE_FEE_LMT_ABBRE, maxLimitSize);
    }

    
    /** 
     * return user's Transfer Amount Limit
     * @return double
     */
    public double getTransferAmountLimit(){
        return this.limits.get(TRN_AMT_LMT_ABBRE);
    }

    /** 
     * return user's Storage Space Limit
     * @return double
     */
    public double getStorageSpaceLimit(){
        return this.limits.get(STR_SPC_LMT_ABBRE);
    }

    /** 
     * return user's Usage Fee Limit
     * @return double
     */
    public double getUsageFeeLimit(){
        return this.limits.get(USAGE_FEE_LMT_ABBRE);
    }

    /** 
     * return user's plan Limits
     * @param abbreviation
     * @return double
     */
    public double getLimits(char abbreviation){
        return this.limits.get(abbreviation);
    }

    /** 
     * return user's plan Minimum Limits
     * @param abbreviation
     * @return double
     */
    public double getMinimumLimits(char abbreviation){
        return this.minimumLimits.get(abbreviation);
    }

    /** 
     * return user's plan Maximum Limits
     * @param abbreviation
     * @return double
     */
    public double getMaximumLimits(char abbreviation){
        return this.maximumLimits.get(abbreviation);
    }
    
    /** 
     * set the Limit of the specified abbreviation
     * @param abbreviation
     * @param limitAmount
     */
    public void setLimit(char abbreviation, double limitAmount){
        this.limits.put(abbreviation, limitAmount);
    }
}
