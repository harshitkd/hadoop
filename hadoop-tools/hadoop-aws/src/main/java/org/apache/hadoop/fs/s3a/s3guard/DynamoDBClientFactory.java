/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs.s3a.s3guard;

import java.io.IOException;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.s3a.DefaultS3ClientFactory;

import static org.apache.hadoop.fs.s3a.Constants.S3GUARD_DDB_REGION_KEY;
import static org.apache.hadoop.fs.s3a.S3AUtils.createAWSCredentialProviderSet;

/**
 * Interface to create a DynamoDB client.
 *
 * Implementation should be configured for setting and getting configuration.
 */
interface DynamoDBClientFactory extends Configurable {
  Logger LOG = LoggerFactory.getLogger(DynamoDBClientFactory.class);

  /**
   * Create a DynamoDB client object from configuration.
   *
   * The DynamoDB client to create does not have to relate to any S3 buckets.
   * All information needed to create a DynamoDB client is from the hadoop
   * configuration. Specially, if the region is not configured, it will use the
   * provided region parameter. If region is neither configured nor provided,
   * it will indicate an error.
   *
   * @param defaultRegion the default region of the AmazonDynamoDB client
   * @return a new DynamoDB client
   * @throws IOException if any IO error happens
   */
  AmazonDynamoDB createDynamoDBClient(String defaultRegion) throws IOException;

  /**
   * The default implementation for creating an AmazonDynamoDB.
   */
  class DefaultDynamoDBClientFactory extends Configured
      implements DynamoDBClientFactory {
    @Override
    public AmazonDynamoDB createDynamoDBClient(String defaultRegion)
        throws IOException {
      assert getConf() != null : "Should have been configured before usage";

      final Configuration conf = getConf();
      final AWSCredentialsProvider credentials =
          createAWSCredentialProviderSet(null, conf, null);
      final ClientConfiguration awsConf =
          DefaultS3ClientFactory.createAwsConf(conf);

      String region = conf.getTrimmed(S3GUARD_DDB_REGION_KEY);
      if (StringUtils.isEmpty(region)) {
        region = defaultRegion;
      }
      Preconditions.checkState(StringUtils.isNotEmpty(region),
          "No DynamoDB region is provided!");
      LOG.debug("Creating DynamoDB client in region {}", region);

      return AmazonDynamoDBClientBuilder.standard()
          .withCredentials(credentials)
          .withClientConfiguration(awsConf)
          .withRegion(region)
          .build();
    }
  }

}
