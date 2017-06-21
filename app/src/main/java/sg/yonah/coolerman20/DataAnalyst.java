package sg.yonah.coolerman20;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by litangqing on 16/6/17.
 *
 * a Class that can take in numbers and generate
 * statistical summary such as
 * average, max, min, etc.
 *
 * Intending to add this into the auto msg content.
 */

public class DataAnalyst {
    LinkedList<Double> numCollection = new LinkedList<Double>();
    Double max = 0.0;
    Double min = 20.0;
    int count = 0;
    Double sum = 0.0;
    public void addNumber(double inputNum){
        numCollection.add(inputNum);
        if (inputNum > max){
            max = inputNum;
        }
        if (inputNum < min){
            min = inputNum;
        }
        count ++;
        sum += inputNum;
    }
    /*
    return current statistical summary in the form of an arraylist
    -> return average, max, min in order.
     */
    public ArrayList<Double> getStatsSummary() {
        ArrayList<Double> result = new ArrayList<Double>();
        Double avg = 0.0;
        try{avg = sum/count;} catch (ArithmeticException e){e.printStackTrace();}
        result.add(avg);
        result.add(max);
        result.add(min);
        return result;
    }
}
