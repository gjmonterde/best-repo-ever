package track;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.HashMap;

public class JinDori {
    private int n, m, k;
    private int[] c;
    private String roadAsString;
    private List<Integer> newColorsAtTimeK;

    public JinDori(String[] lines){
        // First line of input.in (n, m, k)
        String[] lineZero = lines[0].split(" ");
        this.n = Integer.parseInt(lineZero[0]);
        this.m = Integer.parseInt(lineZero[1]);
        this.k = Integer.parseInt(lineZero[2]);
        // Second line of input.in (c[1] ...c[n])
        this.c = Arrays.stream(lines[1].split(" ")).mapToInt(Integer::parseInt).toArray();

        if((this.n < 2 || this.n > 1000) || (this.c.length < 1 && this.c.length > this.n) || (this.m < 1 || this.m > 2000))
            throw new ArrayIndexOutOfBoundsException("Number of Points(n), Number of Colors(c) or Number of Roads(m) is out of bounds.");
        
        /**
         * u[1] v[1]
         * .
         * .
         * .
         * u[m] v[m]
         */
        List<String> roadsList = new ArrayList<String>(Arrays.asList(lines)).subList(2, lines.length);

        if((roadsList.size() != this.m))
            throw new ArrayIndexOutOfBoundsException("Number of Roads(m) is out of bounds.");

        // Format roads to a single String with ';' as delimiter 
        // Formatted Road: u[1] v[1];u[2] v[2]; ...u[m] v[m]
        this.roadAsString = String.join(";", roadsList);
        this.newColorsAtTimeK = new ArrayList<>(this.getColorAtTimeK(roadAsString, n, k, c));
    }

    /** 
     * Return the color of each point at time (k) to the caller class (App.java)
     * @return String[]
     */
    public String[] outputLines(){
        return newColorsAtTimeK.stream().map(String::valueOf).collect(Collectors.toList()).toArray(new String[newColorsAtTimeK.size()]);
    }
    
    /** 
     * Get the color of each point at time (k)
     * @param roadAsString Roads u[1] v[1];u[2] v[2]; ...u[m] v[m]
     * @param n number of points
     * @param k max time
     * @param c initial color of each point
     * @return List<Integer> : color of each point at max time (k)
     */
    private List<Integer> getColorAtTimeK(String roadAsString, int n, int k, int[] c){
        List<Set<Integer>> adjacentPointsPerPoint = new ArrayList<>();
        
        // Iterate each point and get its adjacent points from the roads
        for(int point=1; point<=n; point++){
            // Regex pattern to identify the roads from "roadAsString" where the current point is connected
            Pattern pattern = Pattern.compile("((^|(?<=;))"+ point +" \\d*)|(\\d* "+ point +"((?=;)|$))");
            Matcher matcher = pattern.matcher(roadAsString);

            Set<Integer> adjacentPoints = new HashSet<Integer>();
            final int currentPoint = point;
            // Iterate each String matched by regex pattern
            while (matcher.find()) {
                // Split the String, parse to int and get the point that is not the current point of the loop
                int adjacentPoint = Arrays.stream(matcher.group().split(" "))
                    .mapToInt(Integer::parseInt)
                    .filter(num -> num != currentPoint).findFirst().getAsInt();
                // Add it as adjacent points
                adjacentPoints.add(adjacentPoint);
            }
            // Add the collected adjacent points as adjacent points of the current point
            adjacentPointsPerPoint.add(adjacentPoints);
        }
        
        // Stores the new colors of each point
        // Initialize by initial color of each point (int[] c)
        List<Integer> newC = Arrays.stream(c).boxed().collect(Collectors.toList());
        // Iterate until the max time k
        for(int time=1; time <= k; time++){
            List<Integer> colorsAtCurrentTime = new ArrayList<Integer>();
            // Iterate over each point, proceed to getting new color
            for(Set<Integer> adjacentPointsSet : adjacentPointsPerPoint){
                // colorMap (key: color of adjacent point, value: number of frequency)
                Map<Integer,Integer> colorMap =new HashMap<Integer, Integer>();

                // Iterate each adjacent point of the current point
                for(int adjacentPoint : adjacentPointsSet){
                    int index = adjacentPoint - 1, frequency = 1;

                    // Identify the color of adjacent point and store it to colorMap
                    // If identified color is already stored, add its frequency by 1
                    // Else store the new color and initialize its frequency by 1
                    if(colorMap.keySet().contains(newC.get(index))){
                        frequency = colorMap.get(newC.get(index));
                        colorMap.put(newC.get(index), ++frequency);
                    }else
                        colorMap.put(newC.get(index), frequency);
                } 

                // Get the value of largest frequency of the colorMap
                int maxFrequency=(Collections.max(colorMap.values()));

                // Get all colors in "colorMap" with the largest frequency
                // Sort all equally frequent colors to ascending order 
                // Assign the first color as new color of the curren point
                Integer newColor = colorMap.entrySet().stream()
                    .filter(entry -> maxFrequency == entry.getValue())
                    .map(Map.Entry::getKey).sorted()
                    .mapToInt(Integer::intValue).findFirst().getAsInt();

                // Store the new color of current point
                colorsAtCurrentTime.add(newColor);
            }
            // Replace the stored colors to new color for current time
            newC.clear();
            newC.addAll(colorsAtCurrentTime);
        }

        return newC;
    }
}