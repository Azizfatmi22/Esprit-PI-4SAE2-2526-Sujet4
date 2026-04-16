package com.example.mstrainerhiring.utils;

import com.example.mstrainerhiring.dto.JobDTO;
import com.example.mstrainerhiring.enums.Technology;

public class TemplateGenerator {

    public static JobDTO generateTemplate(Technology tech) {
        JobDTO template = new JobDTO();
        template.setTechnology(tech);
        template.setMinExperience(3);
        template.setMaxExperience(6);
        template.setSalaryRange("2500 - 4000");

        String title;
        String desc;

        switch (tech) {
            case JAVA:
                title = "Senior Java Back-end Engineer";
                desc = "We are looking for an experienced Java developer to join our core backend team.\n\n" +
                       "Key Responsibilities:\n" +
                       "- Design and implement scalable microservices using Spring Boot.\n" +
                       "- Optimize application performance and ensure code quality.\n" +
                       "- Collaborate closely with front-end teams for API integration.\n\n" +
                       "Requirements:\n" +
                       "- Strong proficiency in Java 17+ and Spring framework.\n" +
                       "- Solid understanding of JPA, Hibernate, and SQL databases.\n" +
                       "- Experience with CI/CD, Git, and Agile methodologies.";
                break;
            case PYTHON:
                title = "Python Data Scientist / Engineer";
                desc = "Join our data science team to build predictive models and analytics pipelines.\n\n" +
                       "Key Responsibilities:\n" +
                       "- Develop and maintain data processing pipelines.\n" +
                       "- Implement machine learning algorithms using Python.\n" +
                       "- Collaborate with product managers to deliver data-driven insights.\n\n" +
                       "Requirements:\n" +
                       "- Proficiency in Python and libraries like Pandas, NumPy, Scikit-learn.\n" +
                       "- Experience with visualization tools and SQL.\n" +
                       "- Strong analytical and problem-solving skills.";
                break;
            case JAVASCRIPT:
            case TYPESCRIPT:
                title = "Frontend Developer (" + tech.name() + ")";
                desc = "We are seeking a talented frontend engineer to craft stunning user interfaces.\n\n" +
                       "Key Responsibilities:\n" +
                       "- Build highly responsive UI components using modern frameworks (Angular/React).\n" +
                       "- Ensure cross-browser compatibility and optimize rendering speed.\n" +
                       "- Work closely with UX/UI designers to implement 'Masterpiece' designs.\n\n" +
                       "Requirements:\n" +
                       "- Expert knowledge of HTML, CSS, and " + tech.name() + ".\n" +
                       "- Experience with state management and RESTful API consumption.\n" +
                       "- A keen eye for visual details and micro-animations.";
                break;
            default:
                title = "Experienced " + tech.name() + " Developer";
                desc = "We are expanding our technical team and looking for a dedicated " + tech.name() + " specialist.\n\n" +
                       "Key Responsibilities:\n" +
                       "- Write clean, maintainable, and efficient code.\n" +
                       "- Participate in code reviews and architecture design.\n" +
                       "- Troubleshoot, debug and upgrade existing software.\n\n" +
                       "Requirements:\n" +
                       "- Proven experience as a " + tech.name() + " developer.\n" +
                       "- Familiarity with modern software development practices.\n" +
                       "- Excellent communication skills and teamwork spirit.";
                break;
        }

        template.setTitle(title);
        template.setDescription(desc);

        return template;
    }
}
