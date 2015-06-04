/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.device.mgt.iot.dao.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.device.mgt.iot.common.IotDeviceMgtPluginException;
import org.wso2.carbon.device.mgt.iot.common.config.datasource.JNDILookupDefinition;
import org.wso2.carbon.device.mgt.iot.common.config.datasource.IotDataSourceConfig;
import org.wso2.carbon.device.mgt.iot.util.IotDeviceManagementSchemaInitializer;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.List;

/**
 * Utility method required by IotDeviceManagement DAO classes.
 */
public class IotDeviceManagementDAOUtil {

    private static final Log log = LogFactory.getLog(IotDeviceManagementDAOUtil.class);

    public static DataSource lookupDataSource(String dataSourceName,
                                              final Hashtable<Object, Object> jndiProperties){

        try {
            if (jndiProperties == null || jndiProperties.isEmpty()) {
                return (DataSource) InitialContext.doLookup(dataSourceName);
            }
            final InitialContext context = new InitialContext(jndiProperties);
            return (DataSource) context.lookup(dataSourceName);
        } catch (Exception e) {
            String msg = "Error in looking up data source: " + e.getMessage();
            log.error(msg, e);
            throw new RuntimeException(msg + e.getMessage(), e);
        }
    }

    public static void cleanupResources(Connection conn, PreparedStatement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.warn("Error occurred while closing result set", e);
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.warn("Error occurred while closing prepared statement", e);
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.warn("Error occurred while closing database connection", e);
            }
        }
    }

    public static void cleanupResources(PreparedStatement stmt, ResultSet rs) {
        cleanupResources(null, stmt, rs);
    }

    /**
     * Creates the iot device management schema.
     *
     * @param dataSource Iot data source
     */
    public static void setupIotDeviceManagementSchema(DataSource dataSource) throws IotDeviceMgtPluginException {
        IotDeviceManagementSchemaInitializer initializer =
                new IotDeviceManagementSchemaInitializer(dataSource);
        log.info("Initializing iot device management repository database schema");
        try {
            initializer.createRegistryDatabase();
        } catch (Exception e) {
            throw new IotDeviceMgtPluginException("Error occurred while initializing Iot Device " +
                                                "Management database schema", e);
        }
    }


    /**
     * Resolve data source from the data source definition
     *
     * @param config data source configuration
     * @return data source resolved from the data source definition
     */
    private static DataSource resolveDataSource(IotDataSourceConfig config) {
        DataSource dataSource = null;
        if (config == null) {
            throw new RuntimeException(
                    "data source configuration " + "is null and " +
                            "thus, is not initialized");
        }
        JNDILookupDefinition jndiConfig = config.getJndiLookupDefinition();
        if (jndiConfig != null) {
            if (log.isDebugEnabled()) {
                log.debug("Initializing data source using the JNDI " +
                        "Lookup Definition");
            }
            List<JNDILookupDefinition.JNDIProperty> jndiPropertyList =
                    jndiConfig.getJndiProperties();
            if (jndiPropertyList != null) {
                Hashtable<Object, Object> jndiProperties = new Hashtable<Object, Object>();
                for (JNDILookupDefinition.JNDIProperty prop : jndiPropertyList) {
                    jndiProperties.put(prop.getName(), prop.getValue());
                }
                dataSource = IotDeviceManagementDAOUtil.lookupDataSource(jndiConfig.getJndiName(), jndiProperties);
            } else {
                dataSource = IotDeviceManagementDAOUtil.lookupDataSource(jndiConfig.getJndiName(), null);
            }
        }
        return dataSource;
    }
}
