/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive_grid_cloud_portal.cli.cmd.rm;

import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED;
import static org.ow2.proactive_grid_cloud_portal.cli.HttpResponseStatus.OK;

import org.apache.http.client.methods.HttpPost;
import org.ow2.proactive_grid_cloud_portal.cli.ApplicationContext;
import org.ow2.proactive_grid_cloud_portal.cli.CLIException;
import org.ow2.proactive_grid_cloud_portal.cli.cmd.AbstractCommand;
import org.ow2.proactive_grid_cloud_portal.cli.cmd.Command;
import org.ow2.proactive_grid_cloud_portal.cli.utils.HttpResponseWrapper;
import org.ow2.proactive_grid_cloud_portal.cli.utils.QueryStringBuilder;


public class RemoveNodeSourceCommand extends AbstractCommand implements Command {
    private String nodeSource;

    private boolean preempt;

    public RemoveNodeSourceCommand(String nodeSource) {
        this(nodeSource, Boolean.toString(false));
    }

    public RemoveNodeSourceCommand(String nodeSource, String preempt) {
        this.nodeSource = nodeSource;
        this.preempt = Boolean.valueOf(preempt);
    }

    public void execute(ApplicationContext currentContext) throws CLIException {
        if (currentContext.isForced()) {
            preempt = true;
        }
        HttpPost request = new HttpPost(currentContext.getResourceUrl("nodesource/remove"));
        QueryStringBuilder queryStringBuilder = new QueryStringBuilder();
        queryStringBuilder.add("name", nodeSource).add("preempt", Boolean.toString(preempt));
        request.setEntity(queryStringBuilder.buildEntity(APPLICATION_FORM_URLENCODED));
        HttpResponseWrapper response = execute(request, currentContext);
        if (statusCode(response) == statusCode(OK)) {
            boolean success = readValue(response, Boolean.TYPE, currentContext);
            resultStack(currentContext).push(success);
            if (success) {
                writeLine(currentContext, "Node source '%s' deleted successfully.", nodeSource);
            } else {
                writeLine(currentContext, "Cannot delete node source: %s.", nodeSource);

            }
        } else {
            handleError(String.format("An error occurred while deleting node source: %s", nodeSource),
                        response,
                        currentContext);
        }

    }

}
