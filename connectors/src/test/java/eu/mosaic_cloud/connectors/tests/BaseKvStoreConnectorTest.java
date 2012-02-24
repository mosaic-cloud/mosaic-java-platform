/*
 * #%L
 * mosaic-connectors
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.connectors.tests;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import eu.mosaic_cloud.connectors.kvstore.BaseKvStoreConnector;
import eu.mosaic_cloud.drivers.interop.kvstore.KeyValueStub;

public abstract class BaseKvStoreConnectorTest<Connector extends BaseKvStoreConnector<String, ?>>
        extends BaseConnectorTest<Connector, BaseKvStoreConnectorTest.Context> {

    protected static class Context extends BaseConnectorTest.Context<KeyValueStub> {

        public String keyPrefix = UUID.randomUUID().toString();
    }

    @Override
    @Test
    public void test() {
        this.testConnector();
        this.testSet();
        this.testGet();
        this.testList();
        this.testDelete();
    }

    protected void testDelete() {
        final String k1 = this.context.keyPrefix + "_key_fantastic";
        final String k2 = this.context.keyPrefix + "_key_fabulous";
        Assert.assertTrue(this.awaitBooleanOutcome(this.connector.delete(k1)));
        Assert.assertFalse(this.awaitBooleanOutcome(this.connector.delete(k2)));
        Assert.assertNull(this.awaitOutcome(this.connector.get(k1)));
        Assert.assertNull(this.awaitOutcome(this.connector.get(k2)));
    }

    protected void testGet() {
        final String k1 = this.context.keyPrefix + "_key_fantastic";
        Assert.assertEquals("fantastic", this.awaitOutcome(this.connector.get(k1)));
    }

    protected void testList() {
        Assert.assertNotNull(this.awaitOutcome(this.connector.list()));
    }

    protected void testSet() {
        final String k1 = this.context.keyPrefix + "_key_fantastic";
        final String k2 = this.context.keyPrefix + "_key_famous";
        Assert.assertTrue(this.awaitBooleanOutcome(this.connector.set(k1, "fantastic")));
        Assert.assertTrue(this.awaitBooleanOutcome(this.connector.set(k2, "famous")));
    }
}
