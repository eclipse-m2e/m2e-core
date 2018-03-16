/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.eclipse.m2e.core.internal.project.conversion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.codehaus.plexus.util.dag.Vertex;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.conversion.AbstractProjectConversionParticipant;


/**
 * Sorts a list of {@link AbstractProjectConversionParticipant} (converters).
 * <ul>
 * <li>Declares all converters as vertices of a Directed Acyclic Graph ({@link DAG})</li>
 * <li>Adds edges between each converter and its preceding (see
 * {@link AbstractProjectConversionParticipant#getPrecedingConverterIds()}) and succeeding (see
 * {@link AbstractProjectConversionParticipant#getSucceedingConverterIds()}) converters</li>
 * <li>ignores all unknown converter dependencies</li>
 * <li>Does a topological sort on the graph</li>
 * <li>The sorted list in unmodifiable.</li>
 * </ul>
 * <p>
 * This implementation was inspired by Apache Maven's {@link org.apache.maven.project.ProjectSorter}
 * </p>
 * 
 * @author Fred Bricon
 * @throws CycleDetectedException if a cycle is detected between project conversion participant dependencies
 * @throws DuplicateConversionParticipantException if any project conversion participant ids are duplicated
 * @see DAG
 */
public class ProjectConversionParticipantSorter {

  private List<AbstractProjectConversionParticipant> sortedConverters;

  public ProjectConversionParticipantSorter(List<AbstractProjectConversionParticipant> converters)
      throws CycleDetectedException, DuplicateConversionParticipantException {

    if(converters == null) {
      throw new IllegalArgumentException("converters parameter can not be null");
    }

    if(converters.isEmpty()) {
      this.sortedConverters = Collections.emptyList();
      return;
    }

    if(converters.size() == 1) {
      this.sortedConverters = Collections.singletonList(converters.get(0));
      return;
    }

    DAG dag = new DAG();

    Map<String, AbstractProjectConversionParticipant> converterMap = new HashMap<String, AbstractProjectConversionParticipant>(
        converters.size());

    //Create a vertex for each converter. Duplicates not allowed!
    for(AbstractProjectConversionParticipant converter : converters) {
      String converterId = converter.getId();

      AbstractProjectConversionParticipant conflictingConverter = converterMap.put(converterId, converter);

      if(conflictingConverter != null) {
        IStatus error = new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, NLS.bind(
            Messages.ProjectConversion_error_duplicate_conversion_participant, converterId));
        throw new DuplicateConversionParticipantException(error);
      }

      dag.addVertex(converterId);
    }

    //Add edges
    for(Vertex converterVx : dag.getVerticies()) {
      String converterId = converterVx.getLabel();

      AbstractProjectConversionParticipant converter = converterMap.get(converterId);

      //Add edges for all the converters this converter should run after
      String[] predecessors = converter.getPrecedingConverterIds();
      if(predecessors != null) {
        for(String id : predecessors) {
          Vertex predecessor = dag.getVertex(id);
          if(predecessor != null) {
            dag.addEdge(converterVx, predecessor);
          }
        }
      }

      //Add edges for all the converters this converter should run before
      String[] successors = converter.getSucceedingConverterIds();
      if(successors != null) {
        for(String id : successors) {
          Vertex successor = dag.getVertex(id);
          if(successor != null) {
            dag.addEdge(successor, converterVx);
          }
        }
      }

    }

    List<String> sortedConverterIds = TopologicalSorter.sort(dag);

    List<AbstractProjectConversionParticipant> sortedConverters = new ArrayList<AbstractProjectConversionParticipant>(
        converters.size());

    for(String id : sortedConverterIds) {
      sortedConverters.add(converterMap.get(id));
    }
    this.sortedConverters = Collections.unmodifiableList(sortedConverters);
  }

  /**
   * @return a sorted, unmodifiable list of the {@link AbstractProjectConversionParticipant}.
   */
  public List<AbstractProjectConversionParticipant> getSortedConverters() {
    return sortedConverters;
  }

}
