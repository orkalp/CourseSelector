 import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;


 public class CourseSelectorBot extends TelegramLongPollingBot {
	 
	String prevInput;
	String currInput;
	CourseSelector selector;
	Boolean[] avblHours;
	String[] desiredDepartments;
    String[] departmentCodes;
    String sectionToCheck;
    boolean sectionCheckFlag;

	
	public CourseSelectorBot() {
		prevInput = "";
		currInput = "";
		selector = new CourseSelector();
		departmentCodes = selector.getDepartmentCodes();
		sectionToCheck = "";
		sectionCheckFlag = true;
	}
	
    @Override
    public void onUpdateReceived(Update update) {
    	
    	
        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {

        	String message_text = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();
            this.prevInput = this.currInput;
            this.currInput = message_text;
            
            

            System.out.println("Chat id: " + chat_id + '\n' + "PrevInput: " + prevInput + '\n' + "CurrInput: " + currInput + '\n');
            
            if(message_text.equals("/start")) {
	            SendMessage message = new SendMessage() // Create a message object object
	                .setChatId(chat_id)
	                .setText(" Welcome to CourseSelector. This bot basicaly has two ways of usage.\n\n" +
	                		 "1) Give some available hours that you can take a course, give some department codes that you consider taking a course from and get courses that fit into your schedule.\n" + '\n' + 
	                		 
	                		 "2) Give a specific section of some course that you'd like to enroll (but can't, as usually quotas are a bit of a problem) and let the bot constantly check whether there's an available quota.\n" + '\n' +  
	                		 
	                		 "Click: \n " + 
	                		  "/findMeCourses for finding courses that fit into your schedule.\n /start for coming back to this menu.\n /help for getting again a list of commands\n /sectionCheck for having the bot check the availability of a specific section \n");
	            try {
	                execute(message); // Sending our message object to user
	            } catch (TelegramApiException e) {
	                e.printStackTrace();
	            }
            }
            else if(message_text.equals("/findMeCourses")) {
            	SendMessage message = new SendMessage() // Create a message object object
    	                .setChatId(chat_id)
    	                .setText(" Please enter your available hours. Use Mon, Tue, Wed, Thu and Fri for days and let digits denote i'th course of the day. \n" + 
    	                		" e.g. \"Mon 1-8 Wed 5-8 Fri 1-4\" means \" I'm available on Mondays from 1st hour to 8th, Wednesdays 5th to 8th and Fridays 1st to 4th.\" Please comply with this exact format.");
    	            try {
    	                execute(message); // Sending our message object to user
    	            } catch (TelegramApiException e) {
    	                e.printStackTrace();
    	            }
            }
            else if(isHoursStr(message_text) && prevInput.equals("/findMeCourses")) {
            	
            	this.avblHours = selector.boolRepresentationOfInputString(message_text);
            	SendMessage message = new SendMessage() // Create a message object object
    	                .setChatId(chat_id)
    	                .setText("Perfect. Now, give me some department codes that you might consider taking a course from. Please don't forget to use capital letters and leave a space between each department \n" + 
    	                		" e.g. CS SPA AMER");
    	            try {
    	                execute(message); // Sending our message object to user
    	            } catch (TelegramApiException e) {
    	                e.printStackTrace();
    	            }
            }
            else if(isDepartmentStr(message_text) && isHoursStr(prevInput)) {
            	
            	String waitMsg = "All set, ready to go. During fetching, if you haven't seen \"success\" along with course counts, please wait a bit. Bot might get overloaded or crash due to infinitely many reasons. As a rule of thumb, when you're mad at some stuff involving computers, internet or simply technology; spend some time thinking about how magical it is even to have some movement of electrons giving you the result of 2+2.";

                SendMessage message = new SendMessage() // Create a message object object
    	                .setChatId(chat_id)
    	                .setText(waitMsg);
    	            try {
    	                execute(message); // Sending our message object to user
    	            } catch (TelegramApiException e) {
    	                e.printStackTrace();
    	            }
            	desiredDepartments = selector.inputCoursesStringToArray(message_text);
                int counter = 0;
                for(String code: desiredDepartments) {
                		
                		int depSpecificCount = 0;

                        String url = "https://stars.bilkent.edu.tr/homepage/ajax/plainOfferings.php?SEMESTER=20172&COURSE_CODE=" + code;
                        CourseNode[] courses = null;
						try {
							courses = selector.getURLandExtractCourseLines(url, code);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						String connMsg = "Connection established with " + code + ". Here are the courses that you can from these guys: ";

                		SendMessage connMessage = new SendMessage() // Create a message object object
        	                .setChatId(chat_id)
        	                .setText(connMsg);
        	            try {
        	                execute(connMessage); // Sending our message object to user
        	            } catch (TelegramApiException e) {
        	                e.printStackTrace();
        	            }
        	            
                        int i = 0;
                        String sendMsgText = "";
                        
                        if(courses != null) {
	                        while (i < courses.length) {
	                            selector.setAvailabilityValues(courses[i]);
	                            Boolean[] hoursOfCourse = selector.boolRepresentationOfHours(courses[i].hours);
	                            boolean result = selector.schedulesMatch(avblHours, hoursOfCourse);
	                            
	                            if (result) {
	                                ++counter;
	                                depSpecificCount++;
	                                if (courses[i].hours.length() == 0) {
	                                    sendMsgText = String.valueOf(courses[i].code) + " (" + courses[i].name + ") given by " + courses[i].professor + ":" + '\n' + '\t' + "This course doesn't have any designated hours OR it's hours haven't been determined yet.";
	                                    sendMsgText += '\n' + "\tMust: " + courses[i].mustQuota + " Elective: " + courses[i].electiveQuota + " Merged: " + courses[i].mergedQuota;
	                                    SendMessage mess = new SendMessage() // Create a message object object
	    	                	                .setChatId(chat_id)
	    	                	                .setText(sendMsgText);
	    	                	            try {
	    	                	                execute(mess); // Sending our message object to user
	    	                	            } catch (TelegramApiException e) {
	    	                	                e.printStackTrace();
	    	                	            }
  	           
	                                } else {
	                                    sendMsgText = String.valueOf(courses[i].code) + " (" + courses[i].name + ") given by " + courses[i].professor + ":" + '\n' + '\t' + "Hours and classrooms: " + courses[i].hours;
	                                    sendMsgText += '\n' + "\tMust: " + courses[i].mustQuota + " Elective: " + courses[i].electiveQuota + " Merged: " + courses[i].mergedQuota;
	                                    SendMessage message2 = new SendMessage() // Create a message object object
	    	                	                .setChatId(chat_id)
	    	                	                .setText(sendMsgText);
	    	                	            try {
	    	                	                execute(message2); // Sending our message object to user
	    	                	            } catch (TelegramApiException e) {
	    	                	                e.printStackTrace();
	    	                	            }
	                                }
	                                
	                                if(!selector.isAvailable(courses[i])) {
	                	            	
	                	            	SendMessage mess2 = new SendMessage() // Create a message object object
	    	                	                .setChatId(chat_id)
	    	                	                .setText("Unfortunately, there's no quota for " + courses[i].name + " as for now. ");
	    	                	            try {
	    	                	                execute(mess2); // Sending our message object to user
	    	                	            } catch (TelegramApiException e) {
	    	                	                e.printStackTrace();
	    	                	            }
	                	            }
	                            }
	                            
	                            ++i;
	                            
	                	            
	                	        sendMsgText = "";
	                        }
                        }
                        
                        if(depSpecificCount == 0) { //courses array is null
                        	SendMessage message2 = new SendMessage() 
                	                .setChatId(chat_id)
                	                .setText("Unfortunately, there aren't any courses from " + code + " that fit into your schedule or " + code +" isn't a legit department code. Either way, your bot thinks that it's time to try something new. ");
                	            try {
                	                execute(message2); 
                	            } catch (TelegramApiException e) {
                	                e.printStackTrace();
                	            }
                        }
                    
                }//end of fetching courses
                
                if (desiredDepartments.length == 1) {
                	SendMessage lastMsg = new SendMessage() // Create a message object object
        	                .setChatId(chat_id)
        	                .setText("Success! Printed a total of " + counter + " courses from " + desiredDepartments[0] + " department");
        	            try {
        	                execute(lastMsg); // Sending our message object to user
        	            } catch (TelegramApiException e) {
        	                e.printStackTrace();
        	            }
                }
                if (desiredDepartments.length > 1) {
                	SendMessage lastMsg2 = new SendMessage() // Create a message object object
        	                .setChatId(chat_id)
        	                .setText("Success! Printed a total of " + counter + " courses from " + desiredDepartments.length + " departments.");
        	            try {
        	                execute(lastMsg2); // Sending our message object to user
        	            } catch (TelegramApiException e) {
        	                e.printStackTrace();
        	            }
                }
                
            }
            else if(isThankYouStr(message_text)) {
            	
            	SendMessage message = new SendMessage() // Create a message object object
    	                .setChatId(chat_id)
    	                .setText("De nada :)");
    	            try {
    	                execute(message); // Sending our message object to user
    	            } catch (TelegramApiException e) {
    	                e.printStackTrace();
    	            }
            }
            
            else if(message_text.equals("/surprisePic")) {
            	
            	SendPhoto atalarPhoto = new SendPhoto()
                        .setChatId(chat_id)
                        .setPhoto("http://www.egitimajansi.com/assets/uploads/other/images/yk7.jpg");
                try {
                    sendPhoto(atalarPhoto); // Call method to send the photo
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            	
            	
            }
            else if(message_text.equals("/help")) {
            	
            	SendMessage message = new SendMessage() // Create a message object object
    	                .setChatId(chat_id)
    	                .setText("It seems like you're stuck. Let's go over the basics.\n" + 
    	                		  "/findMeCourses for finding courses that fit into your schedule.\n /surprisePic for a heart-warming image.\n /start for coming back to this menu.\n /help for getting again a list of commands\n /sectionCheck for checking a section as the name suggests.\n For anything else, @orkalp \n");
    	            try {
    	                execute(message); // Sending our message object to user
    	            } catch (TelegramApiException e) {
    	                e.printStackTrace();
    	            }
            	
            }
            else if(message_text.equals("/sectionCheck")) {
            	SendMessage message = new SendMessage() // Create a message object object
    	                .setChatId(chat_id)
    	                .setText("Alright, now give me a specific section in a format of \"depCode space courseCode minusSign sectionCode\"(e.g. MATH 102-4 or SPA 211-1).");
    	            try {
    	                execute(message); // Sending our message object to user
    	            } catch (TelegramApiException e) {
    	                e.printStackTrace();
    	            }
            	
            }
            
            else if(message_text.equals("/stopCheck")) {
            	sectionCheckFlag = false;
            	SendMessage mess = new SendMessage() // Create a message object object
      	               .setChatId(chat_id)
      	               .setText("Stopped the automated section check for " + this.sectionToCheck);
 	     	    try {
 	     	        execute(mess); // Sending our message object to user
 	            } catch (TelegramApiException e) {
 	    	        e.printStackTrace();
 	    	    }
            	
            }
            
            else if (prevInput.equals("/sectionCheck") && isSectionStr(message_text)){         	
            		
            	this.sectionToCheck = message_text;
            	this.sectionCheckFlag = true;
            	
            	SendMessage mess = new SendMessage() // Create a message object object
     	               .setChatId(chat_id)
     	               .setText("Checking availability of " + message_text);
	     	    try {
	     	        execute(mess); // Sending our message object to user
	            } catch (TelegramApiException e) {
	    	        e.printStackTrace();
	    	    }
                	String[] arr = message_text.split(" ");
	            	String department = arr[0];
	                String url = "https://stars.bilkent.edu.tr/homepage/ajax/plainOfferings.php?SEMESTER=20172&COURSE_CODE=" + department;
	               
	                int interval = 30000; //30 seconds
	                Timer timer = new Timer();
	                timer.scheduleAtFixedRate(new TimerTask(){
	                	
	                	private volatile boolean flag = true;
	                	private volatile int count = 1;

	                    public void stopTimer() {
	                        this.flag = false;
	                    }
	                    
	                	@Override
	                    public void run() {
	                		
	                		while(flag && sectionCheckFlag) { //while not found and not interrupted by user
	                		//connect to department's offerings page
	    	                Document doc = null;
	    					try {
	    						if(count > 1) {
	    						Thread.sleep(10000); //wait for 10 seconds
	    						}
	    						doc = Jsoup.connect(url).timeout(30000).get(); //30 secs timeout
	    						if(count == 1) {
	    						SendMessage mess2 = new SendMessage() // Create a message object object
	    			     	               .setChatId(chat_id)
	    			     	               .setText("Successfully connected to " + department + " offerings page.");
	    				     	    try {
	    				     	        execute(mess2); // Sending our message object to user
	    				            } catch (TelegramApiException e) {
	    				    	        e.printStackTrace();
	    				    	    }
	    						} //print connMsg for only 1 time

	    					} catch (IOException | InterruptedException e1) {
	    						// TODO Auto-generated catch block
	    						e1.printStackTrace();
	    						continue;
	    					}
	    					
	    	                        Elements lines = doc.getElementsByTag("tr");
	    	    	     	    	boolean courseExists = false;

	    	                        for (Element line : lines) {
	    	                        	
	    	                            if (line.text().toString().startsWith(message_text)) {
	    		                            	
	    	                            	courseExists = true;
	    		                            Element availability = (Element)line.select("td").get(12);
	    		                            String availabilityStr = availability.text().toString();
	    		                            CourseNode node = new CourseNode(message_text);
	    		                            node.availabilityString = availabilityStr;
	    		                            selector.setAvailabilityValues(node);
	    		                            
	    	                            	count++; //count of successful connections

	    	                            	
	    		                            if (selector.isAvailable(node)) {
	    		                            	
	    		                            	System.out.println("Found!");
	    		                            	SendMessage mess3 = new SendMessage() // Create a message object object
	    		    	                	               .setChatId(chat_id)
	    		    	                	               .setText(message_text + " is available! \n Must quota: " + node.mustQuota + "\n Elective quota: " + node.electiveQuota + "\n Merged quota: " + node.mergedQuota);
	    		    	                	    try {
	    		    	                	        execute(mess3); // Sending our message object to user
	    		    	                	        stopTimer(); //quota found, flag false, finish execution
	    		    	                        } catch (TelegramApiException e) {
	    		    	               	            e.printStackTrace();
	    		    	               	        }
	    		                           	}
	    		                            else {
	    		                            	System.out.println("Not found :( " + count + "'th trial.");
	    		                            	if(count <= 2) {
	    		    	    						SendMessage mess2 = new SendMessage() // Create a message object object
	    		    	    			     	               .setChatId(chat_id)
	    		    	    			     	               .setText("Not found :( Bot started checking availability. Will inform you when there's quota.");
	    		    	    				     	    try {
	    		    	    				     	        execute(mess2); // Sending our message object to user
	    		    	    				            } catch (TelegramApiException e) {
	    		    	    				    	        e.printStackTrace();
	    		    	    				    	    }
	    		    	    						} //print notFound for only 1 time
	    		                            }
	    		                            	
	    		                           }
	    	                            
	    	                           }
	    	                       
	    	                       
	    	                       
	    	                       if(!courseExists) {
	    	                    	   sectionCheckFlag = false;
	    	                    	   SendMessage mess4 = new SendMessage() // Create a message object object
	    	                 	               .setChatId(chat_id)
	    	                 	               .setText("Couldn't find course. Click /sectionCheck for trying again.");
	    	                 	    try {
	    	                 	        execute(mess4); // Sending our message object to user
	    	                         } catch (TelegramApiException e) {
	    	                	            e.printStackTrace();
	    	                	     }
	    	                    }
	                		}
	                	}
	                }, 0, (long)interval);
	                
            	
            }
            
            else {
            	SendMessage mess4 = new SendMessage() // Create a message object object
     	               .setChatId(chat_id)
     	               .setText("Undefined input. Click /start for going back to initial menu. 	");
     	    try {
     	        execute(mess4); // Sending our message object to user
             } catch (TelegramApiException e) {
    	            e.printStackTrace();
    	        }
            	
            }
            

        }//end "if update is text"
    }//end onUpdateRecieved
    
    public boolean isSectionStr(String str) {
    	
    	
    	String pattern = "";
    	for(int i=0; i<this.departmentCodes.length-1; i++) {
    		pattern += departmentCodes[i] + " \\d+-\\d+" + "|";
    	}
    	pattern += departmentCodes.length-1 + " \\d+-\\d+";
    	Pattern p = Pattern.compile(pattern);
    	Matcher m = p.matcher(str);
      	return m.find();
   	
   }
    
    public boolean isHoursStr(String str) {
    	 Pattern p = Pattern.compile("Mon \\d-\\d|Tue \\d-\\d|Wed \\d-\\d|Thu \\d-\\d|Fri \\d-\\d");
    	 Matcher m = p.matcher(str);
    	 return m.find();
    	
    }
    
    public boolean isDepartmentStr(String str) {
    	String pattern = "";
    	for(int i=0; i<this.departmentCodes.length; i++) {
    		pattern += departmentCodes[i] + '|';
    	}
    	
    	pattern = pattern.substring(0, pattern.length()-1);
    	Pattern p = Pattern.compile(pattern);
    	Matcher m = p.matcher(str);
   	 	return m.find();
    }
    
    public boolean isThankYouStr(String str) {
    	Pattern p = Pattern.compile("thank|Thank|tnx|TNX|Eyv|eyv|Eyw|eyw");
    	Matcher m = p.matcher(str);
   	 	return m.find();
    }
    
    @Override
    public String getBotUsername() {
        // Return bot username
        // If bot username is @MyAmazingBot, it must return 'MyAmazingBot'
        return "CourseSelectorBot";
    }

    @Override
    public String getBotToken() {
        // Return bot token from BotFather
        return "botTokenForYourBot";
    }
}
