import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//purely java, core functions
public class CourseSelector {
    
    
	//gets a URL and a department code
	//returns an array of courses that this department offers
    public CourseNode[] getURLandExtractCourseLines(String url, String department) throws IOException {
    	//connect to department's offerings page
        Document doc = Jsoup.connect((String)url).timeout(0).get();
        //split html by individual lines
        Elements lines = doc.getElementsByTag("tr");
        ArrayList<CourseNode> courses = new ArrayList<CourseNode>();

        for (Element line : lines) {
            if (line.text().toString().startsWith(department)) {
            	
	            Element courseCode = line.select("td").get(0);
	            Element courseName = line.select("td").get(1);
	            Element professor = line.select("td").get(2);
	            Element hours = line.select("td").get(14);
	            String hoursStr = hours.text().toString();
	            Element availability = line.select("td").get(12);
	            String availabilityStr = availability.text().toString();
	            //create new courseNode with properties defined above
	            CourseNode node = new CourseNode(courseCode.text().toString(), courseName.text().toString(), professor.text().toString(), hoursStr, availabilityStr);
	            //add this newly created node to the arraylist of courses
	            courses.add(node);
            }
        }
        //convert arraylist to an array and return it
        CourseNode[] result = courses.toArray(new CourseNode[courses.size()]);
        return result;
    }

    //gets two boolean arrays, input hours and course hours. 
    //returns true given that they don't conflict
    public  boolean schedulesMatch(Boolean[] inputHours, Boolean[] courseHours) {
        int i = 0;
        while (i < courseHours.length) {
            if (courseHours[i].booleanValue() && !inputHours[i].booleanValue()) {
                return false;
            }
            ++i;
        }
        return true;
    }

    //gets a string of desired department codes, splits them and puts in an array
    //returns that array
    public String[] inputCoursesStringToArray(String str) {
        String[] courses = str.split(" ");
        return courses;
    }
    
    //gets a string from the user, representing avbl hours
    //returns a 51-element bool array of avbl hours
    public Boolean[] boolRepresentationOfInputString(String inputString) {
        Boolean[] result = new Boolean[51];
        int i = 0;
        while (i < result.length) {
            result[i] = false;
            ++i;
        } //all slots of return array are at first false (i.e empty)
        
        //first element dummy just to make ith day == ith element
        String[] days = new String[]{"dummy", "Mon", "Tue", "Wed", "Thu", "Fri"};
        int firstFreeHour = 0;
        int lastFreeHour = 0;
        int i2 = 0;
        //iterate through the input string, up until length-6'th char
        while (i2 < inputString.length() - 6) {
            int daysIndex = 0;
            while (daysIndex < days.length) {
                if (inputString.substring(i2, i2 + 3).equals(days[daysIndex])) {
                	//define first and last free hours on a given day
                    firstFreeHour = Character.getNumericValue(inputString.charAt(i2 + 4));
                    lastFreeHour = Character.getNumericValue(inputString.charAt(i2 + 6));
                    //what is the order of them relative to the entire week?
                    int startRelativeToWeek = 10 * (daysIndex - 1) + firstFreeHour;
                    int endRelativeToWeek = 10 * (daysIndex - 1) + lastFreeHour;
                    //make all slots corresponding to the course hours true
                    int k = startRelativeToWeek;
                    while (k <= endRelativeToWeek) {
                        result[k] = true;
                        ++k;
                    }
                }
                ++daysIndex;
            }
            ++i2;
        }
        return result;
    }

    //gets a hours string of a particular course
    public Boolean[] boolRepresentationOfHours(String hoursStr) {
        Boolean[] hours = new Boolean[51];
        int i = 0;
        while (i < hours.length) {
            hours[i] = false;
            ++i;
        } //all slots initially false
        
        String[] days = new String[]{"dummy", "Mon", "Tue", "Wed", "Thu", "Fri"};
        String[] startHours = new String[]{"dummy", "8:40", "9:40", "10:40", "11:40", "13:40", "14:40", "15:40", "16:40", "17:40", "18:40"};
        String[] endHours = new String[]{"dummy", "9:30", "10:30", "11:30", "12:30", "14:30", "15:30", "16:30", "17:30", "18:30", "19:30"};
        int i2 = 0;
        while (i2 < hoursStr.length() - 15) {
            int daysIndex = 0;
            //do for each day defined above
            while (daysIndex < days.length) {
            	//if there's sth for a given day do
                if (hoursStr.substring(i2, i2 + 3).equals(days[daysIndex])) {
                    String hourPartOfDay = hoursStr.substring(i2 + 4, i2 + 15);
                    String[] arr = hourPartOfDay.split("-");
                    String start = arr[0];
                    String end = arr[1];
                    if (end.charAt(end.length() - 1) == ' ') {
                        end = end.substring(0, end.length() - 1);
                    }
                    //in which hours does the course start and end
                    int startsAtIndex = orderOfOccurenceOfStringInArray(start, startHours);
                    int endsAtIndex = orderOfOccurenceOfStringInArray(end, endHours);
                    //what are these hours relative to the entire week
                    int startRelativeToWeek = 10 * (daysIndex - 1) + startsAtIndex;
                    int endRelativeToWeek = 10 * (daysIndex - 1) + endsAtIndex;
                    int k = startRelativeToWeek;
                    while (k <= endRelativeToWeek) {
                        hours[k] = true;
                        ++k;
                    } //make all slots that corresponds to a class/lab true
                }
                ++daysIndex;
            }
            ++i2;
        }
        return hours;
    }

    //gets a string and an array
    //given that the string exists in the array, returns it's location
    public int orderOfOccurenceOfStringInArray(String inputStr, String[] items) {
        int result = -1;
        //workaround for inconsistent entries for 8's and 9's
        if (inputStr.equals("08:40") || inputStr.equals("09:30")) {
            return 1; //first hour of the day
        }
        if (inputStr.equals("09:40")) {
            return 2; //second hour of the day
        }
        if (stringContainsItemFromList(inputStr, items)) {
            int i = 0;
            while (i < items.length) {
                if (items[i].equals(inputStr)) {
                    result = i;
                    break;
                }
                ++i;
            }
        }
        return result;
    }

    //gets a string and an array
    //returns true if the string exists in the array
    public boolean stringContainsItemFromList(String inputStr, String[] items) {
        boolean result = false;
        int i = 0;
        while (i < items.length) {
            if (items[i].equals(inputStr)) {
                result = true;
            }
            ++i;
        }
        return result;
    }

    //extracts dep codes from a non-pre-processed html string and returns dep codes as an array
    public String[] getDepartmentCodes() {
        ArrayList<String> departmentCodes = new ArrayList<String>();
        String rawHTML = "</option><option value='ACC' >ACC</option><option value='ADA' >ADA</option><option value='AMER' >AMER</option><option value='ARCH' >ARCH</option><option value='BF' >BF</option><option value='BIM' >BIM</option><option value='BTE' >BTE</option><option value='CAA' >CAA</option><option value='CAD' >CAD</option><option value='CHEM' >CHEM</option><option value='CI' >CI</option><option value='CINT' >CINT</option><option value='CITE' >CITE</option><option value='COMD' >COMD</option><option value='CS' >CS</option><option value='CTE' >CTE</option><option value='CTIS' >CTIS</option><option value='CTP' >CTP</option><option value='DIR' >DIR</option><option value='ECON' >ECON</option><option value='EDEB' >EDEB</option><option value='EE' >EE</option><option value='EEE' >EEE</option><option value='EEPS' >EEPS</option><option value='ELIT' >ELIT</option><option value='ELS' >ELS</option><option value='EM' >EM</option><option value='EMBA' >EMBA</option><option value='ENG' >ENG</option><option value='ETE' >ETE</option><option value='ETS' >ETS</option><option value='FA' >FA</option><option value='FRE' >FRE</option><option value='FRL' >FRL</option><option value='FRP' >FRP</option><option value='GE' >GE</option><option value='GER' >GER</option><option value='GIA' >GIA</option><option value='GRA' >GRA</option><option value='HART' >HART</option><option value='HCIV' >HCIV</option><option value='HIST' >HIST</option><option value='HISTR' >HISTR</option><option value='HUM' >HUM</option><option value='IAED' >IAED</option><option value='IE' >IE</option><option value='IELTS' >IELTS</option><option value='IR' >IR</option><option value='ITA' >ITA</option><option value='JAP' >JAP</option><option value='LAUD' >LAUD</option><option value='LAW' >LAW</option><option value='MAN' >MAN</option><option value='MATH' >MATH</option><option value='MBA' >MBA</option><option value='MBG' >MBG</option><option value='ME' >ME</option><option value='MIAPP' >MIAPP</option><option value='MSC' >MSC</option><option value='MSG' >MSG</option><option value='MSN' >MSN</option><option value='MTE' >MTE</option><option value='MUS' >MUS</option><option value='MUSS' >MUSS</option><option value='NSC' >NSC</option><option value='PE' >PE</option><option value='PHIL' >PHIL</option><option value='PHYS' >PHYS</option><option value='PNT' >PNT</option><option value='POLS' >POLS</option><option value='PREP' >PREP</option><option value='PSYC' >PSYC</option><option value='RUS' >RUS</option><option value='SFL' >SFL</option><option value='SOC' >SOC</option><option value='SPA' >SPA</option><option value='TE' >TE</option><option value='TEFL' >TEFL</option><option value='THEA' >THEA</option><option value='THM' >THM</option><option value='THR' >THR</option><option value='THS' >THS</option><option value='TOEFL' >TOEFL</option><option value='TRIN' >TRIN</option><option value='TRK' >TRK</option><option value='TTP' >TTP</option><option value='TURK' >TURK</option></select>";
        Document doc = Jsoup.parse((String)rawHTML);
        Elements codes = doc.select("option");
        for (Element code : codes) {
            departmentCodes.add(code.text().toString());
        }
        String[] result = departmentCodes.toArray(new String[departmentCodes.size()]);
        return result;
    }
    
    //gets a course node
    //from it's non-processed avblVals string, extracts quota info and sets node's values accordingly
    public void setAvailabilityValues(CourseNode course) {
        String str = course.availabilityString;
        //two possibilities for avbl string, else means unlimited
        String pattern1 = "(\\d+)( Must )(\\d+)( Elect)(\\.)";
        String pattern2 = "(\\d+)( Must or Elect)(\\.)";
        if (str.matches(pattern1)) {
            int temp = 0;
            String mustValueStr = "";
            //start from beginning, iterate while a digit is seen
            while (Character.isDigit(str.charAt(temp))) {
                mustValueStr = String.valueOf(mustValueStr) + str.charAt(temp);
                ++temp;
            }
            //temp is at first non-digit value
            while (!Character.isDigit(str.charAt(temp))) {
                ++temp;
            }
            //iterate till char is not a digit
            String electiveValueStr = "";
            while (Character.isDigit(str.charAt(temp))) {
                electiveValueStr = String.valueOf(electiveValueStr) + str.charAt(temp);
                ++temp;
            }
            course.electiveQuota = Integer.parseInt(electiveValueStr);
            course.mustQuota = Integer.parseInt(mustValueStr);
            course.mergedQuota = 0;
        } 
        else if (str.matches(pattern2)) {
            int temp = 0;
            String mergedValueStr = "";
            while (Character.isDigit(str.charAt(temp))) {
                mergedValueStr = String.valueOf(mergedValueStr) + str.charAt(temp);
                ++temp;
            }
            course.electiveQuota = 0;
            course.mustQuota = 0;
            course.mergedQuota = Integer.parseInt(mergedValueStr);
        } 
        else {
            course.electiveQuota = 0;
            course.mustQuota = 0;
            course.mergedQuota = 999;
        }
    }

    //checks course availability
    public void availabilityCheck(final String courseString) throws IOException {
        String department = "";
        String url = "https://stars.bilkent.edu.tr/homepage/ajax/plainOfferings.php?SEMESTER=20172&COURSE_CODE=";
        String[] arr = courseString.split(" ");
        department = arr[0];
        String URLFINAL = String.valueOf(url) + department;
        int interval = 5000;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask(){

            @Override
            public void run() {
                Document doc = null;
                try {
                    doc = Jsoup.connect(URLFINAL).timeout(0).get();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                Elements lines = doc.getElementsByTag("tr");
                for (Element line : lines) {
                    if (!line.text().toString().startsWith(courseString)) continue;
                    Element availability = (Element)line.select("td").get(12);
                    String availabilityStr = availability.text().toString();
                    CourseNode node = new CourseNode(courseString);
                    node.availabilityString = availabilityStr;
                    setAvailabilityValues(node);
                    if (isAvailable(node)) {
                        System.out.println("Found!");
                        continue;
                    }
                    System.out.println("Not found :(");
                }
            }
        }, interval, (long)interval);
    }

    public boolean isAvailableAsElective(CourseNode course) {
        if (course.electiveQuota > 0 || course.mergedQuota > 0) {
            return true;
        }
        return false;
    }

    public boolean isAvailableAsMust(CourseNode course) {
        if (course.mustQuota > 0) {
            return true;
        }
        return false;
    }

    public boolean isAvailable(CourseNode course) {
        if (isAvailableAsElective(course) || isAvailableAsMust(course)) {
            return true;
        }
        return false;
    }

}
