package org.cloudbus.agent;

import org.jooq.DSLContext;

import java.sql.Connection;

public interface Agent {
    String getName();
    void receiveMessage(AgentMessage message);
    void monitor();
    void analyze();
    void plan(String PowerModel, Connection conn, DSLContext context);
    void execute();
    default void setAgentProgram(Object program) {
        System.out.println("Program set to: " + program.toString());
    }
    double getCurrentTime();
    void setCurrentTime(double lastMAPEloop);
}
