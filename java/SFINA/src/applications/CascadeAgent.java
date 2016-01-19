/*
 * Copyright (C) 2015 SFINA Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package applications;

import java.util.ArrayList;
import java.util.HashMap;
import network.FlowNetwork;
import network.Node;
import org.apache.log4j.Logger;
import protopeer.util.quantities.Time;

/**
 * Cascade if link limits violated. Domain independent.
 * @author Ben
 */
public class CascadeAgent extends BenchmarkDomainAgent{
    
    private static final Logger logger = Logger.getLogger(CascadeAgent.class);
    
    public CascadeAgent(String experimentID, 
            String peersLogDirectory, 
            Time bootstrapTime, 
            Time runTime, 
            String timeTokenName, 
            String experimentConfigurationFilesLocation, 
            String experimentOutputFilesLocation,
            String nodesLocation, 
            String linksLocation, 
            String nodesFlowLocation, 
            String linksFlowLocation, 
            String eventsLocation, 
            String columnSeparator, 
            String missingValue,
            HashMap systemParameters){
        super(experimentID,
                peersLogDirectory,
                bootstrapTime,
                runTime,
                timeTokenName,
                experimentConfigurationFilesLocation,
                experimentOutputFilesLocation,
                nodesLocation,
                linksLocation,
                nodesFlowLocation,
                linksFlowLocation,
                eventsLocation,
                columnSeparator,
                missingValue,
                systemParameters);
    }
    
    /**
     * Implements cascade as a result of overloaded links. Continues until system stabilizes, i.e. no more link overloads occur. Calls mitigateOverload method before finally calling linkOverload method, therefore mitigation strategies can be implemented.
     */
    @Override
    public void runAnalysis(){
        int iter = 0;
        ArrayList<ArrayList<FlowNetwork>> islandBuffer = new ArrayList<>(); // row index is iteration, each entry is island to be treated at this iteration
        islandBuffer.add(getFlowNetwork().computeIslands());
        while(!islandBuffer.get(iter).isEmpty()){
            System.out.println("---------------------\n---- Iteration " + (iter+1) + " ----");
            islandBuffer.add(new ArrayList<>()); // List of islands for next iteration (iter+1)
            for(int i=0; i < islandBuffer.get(iter).size(); i++){ // go through islands at current iteration
                FlowNetwork currentIsland = islandBuffer.get(iter).get(i);
                System.out.println("---> Treating island with " + currentIsland.getNodes().size() + " nodes.");
                
                boolean converged = flowConvergenceStrategy(currentIsland); // do flow analysis
                System.out.println("=> converged " + converged);
                if(converged){
                    
                    // mitigation strategy if implemented
                    mitigateOverload(currentIsland);
                    
                    boolean linkOverloaded = linkOverload(currentIsland);
                    //boolean nodeOverloaded = nodeOverload(currentIsland);
                    System.out.println("=> overloaded " + linkOverloaded);
                    if(linkOverloaded){
                        // add islands of the current island to next iteration
                        for (FlowNetwork net : currentIsland.computeIslands())
                            islandBuffer.get(iter+1).add(net);
                    }
                    else{
                        getTemporalIslandStatus().get(getSimulationTime()).put(currentIsland, true);
                        getFlowNetwork().setIslandConvergence(currentIsland, true);
                    }
                }
                else{
                    getTemporalIslandStatus().get(getSimulationTime()).put(currentIsland, false);
                    getFlowNetwork().setIslandConvergence(currentIsland, false);
                }
            }
            
            // Output network snapshot of current iteration
            setCurrentIteration(iter+1);
            outputNetworkData();
            
            // Go to next iteration if there were islands added to it
            iter++;
        }
    }
}
