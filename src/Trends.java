/*
 * Copyright 2022 Daniel Allen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;

public class Trends {
    /**
     Get the average (mean) value from a list, defined as
     <blockquote>
         <div style="display:table;">
             <div style="vertical-align:middle;transform:translateY(100%);float:left;display:table-cell;width:fit-content;">n<sup>-1</sup></div>
             <div style="vertical-align:middle;float:left;display:table-cell;width:fit-content;">
                 <div style="display:table-row;text-align:center;font-size:0.9em;height:0.9em;">N</div>
                 <div style="display:table-row;text-align:center;font-size:1.4em;">&sum;</div>
                 <div style="display:table-row;text-align:center;font-size:0.9em;height:0.9em;">i=1</div>
             </div>
             <div style="vertical-align:middle;transform:translateY(100%);float:left;display:table-cell;width:fit-content;">a<sub>i</sub></div>
         </div>
     </blockquote>
     * @param doubles The list
     * @return The mean of the numbers
     */
    public static double mean(ArrayList<Double> doubles) {
        if(doubles.size() == 0)
            return 0;
        double sum = 0;
        for(Double d : doubles){
            sum += d;
        }
        return sum/doubles.size();
    }

    /**
     * Simple class to store the data of a linear trend.
     */
    public static class LinearTrend {
        double a;
        double b;

        /**
         * Constructs a Linear Trend based on the slope (<code>a</code>) and intercept (<code>b</code>) of an equation from the form <code>y = ax +
         * b</code>
         * @param a the slope
         * @param b the y-intercept
         */
        public LinearTrend(double a, double b){
            this.a = a;
            this.b = b;
        }

        /**
         * Gets the direction of this trend, indicated by
         * <blockquote>
         *     -1 for decreasing<br>
         *     0 for even<br>
         *     1 for increasing
         * </blockquote>
         * @return Integer representing the direction of this trend
         */
        public int getDirection(){
            return a < 0 ? -1 : a > 0 ? 1 : 0;
        }

        @Override
        public String toString(){
            if(b < 0)
                return a + "x - " + Math.abs(b);
            if(b == 0)
                return a + "x";
            return a + "x + " + b;
        }
    }
    public static LinearTrend getLinearTrend(ArrayList<Double> points){
        int n = points.size();
        //calculate A
        double xy = 0;
        double x = 0;
        double y = 0;
        double x2 = 0;
        for(int i = 0; i < points.size(); i++){
            xy += points.get(i) * i;
            x += points.get(i);
            y += i;
            x2 += points.get(i)*points.get(i);
        }
        double xSum2 = x * x;

        double a = (n*xy-x*y)/(n*x2-xSum2);

        //calculate B
        double b = y - a*x/n;
        return new LinearTrend(a, b);
    }
}
