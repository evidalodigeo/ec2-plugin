package hudson.plugins.ec2;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryRequest;
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryResult;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.SpotPrice;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.Level;

import static org.kohsuke.stapler.Facet.LOGGER;

public final class SpotConfiguration {
    public final String spotMaxBidPrice;

    @DataBoundConstructor
    public SpotConfiguration(String spotMaxBidPrice) {
        this.spotMaxBidPrice = spotMaxBidPrice;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || (this.getClass() != obj.getClass())) {
            return false;
        }
        final SpotConfiguration config = (SpotConfiguration) obj;

        return normalizeBid(this.spotMaxBidPrice).equals(normalizeBid(config.spotMaxBidPrice));
    }

    /**
     * Check if the specified value is a valid bid price to make a Spot request and return the normalized string for the
     * float of the specified bid Bids must be &gt;= .001
     * 
     * @param bid
     *            - price to check
     * @return The normalized string for a Float if possible, otherwise null
     */
    public static String normalizeBid(String bid) {
        try {
            Float spotPrice = Float.parseFloat(bid);

            /* The specified bid price cannot be less than 0.001 */
            if (spotPrice < 0.001) {
                return null;
            }
            return spotPrice.toString();
        } catch (NumberFormatException ex) {
            return null;
        }

    }

    public static boolean isCheaperBid(String currentBid, String configuredBid) {
        try {
            Float spotCurrentPrice = Float.parseFloat(currentBid);
            Float spotBidPrice = Float.parseFloat(configuredBid);

            Float threshold = Float.valueOf(Long.getLong("jenkins.ec2.bidOnDemandThreshold", 95));
            float limit = spotBidPrice * (threshold / 100);
            LOGGER.info("Threshold configured: " + threshold + ", limit: " + limit);
            if (spotCurrentPrice < limit) {
                return true;
            }
        } catch (NumberFormatException ex) {
        }
        return false;
    }

    public static String getCurrentSpotPrice(AmazonEC2 ec2, String zone, InstanceType type, boolean unixSlave) {
        String cp = "";

        if (ec2 != null) {

                try {
                    // Build a new price history request with the currently
                    // selected type
                    DescribeSpotPriceHistoryRequest request = new DescribeSpotPriceHistoryRequest();
                    // If a zone is specified, set the availability zone in the
                    // request
                    // Else, proceed with no availability zone which will result
                    // with the cheapest Spot price
                    if (!StringUtils.isEmpty(zone)) {
                        request.setAvailabilityZone(zone);
                    }

                    HashSet<String> productDescriptions = new HashSet<>();

                    if(unixSlave) {
                        productDescriptions.add("Linux/UNIX");
                    } else {
                        productDescriptions.add("Windows");
                    }
                    request.setProductDescriptions(productDescriptions);

                    Collection<String> instanceType = new ArrayList<String>();
                    instanceType.add(type.toString());
                    request.setInstanceTypes(instanceType);
                    request.setStartTime(new Date());

                    // Retrieve the price history request result and store the
                    // current price
                    DescribeSpotPriceHistoryResult result = ec2.describeSpotPriceHistory(request);

                    if (!result.getSpotPriceHistory().isEmpty()) {

                        SpotPrice currentPrice = result.getSpotPriceHistory().get(0);
                        cp = currentPrice.getSpotPrice();
                    }
                    LOGGER.info("CurrentSpotPrice: " + cp);
                } catch (AmazonServiceException e) {
                    LOGGER.log(Level.FINEST, e.getMessage());
                }
            }
        return cp;
    }

}
