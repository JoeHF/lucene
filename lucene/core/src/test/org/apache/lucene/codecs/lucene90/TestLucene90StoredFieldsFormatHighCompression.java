/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.codecs.lucene90;

import com.carrotsearch.randomizedtesting.generators.RandomPicks;
import java.util.HashSet;
import java.util.Set;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.lucene93.Lucene93Codec;
import org.apache.lucene.codecs.lucene93.Lucene93Codec.Mode;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.index.BaseStoredFieldsFormatTestCase;

public class TestLucene90StoredFieldsFormatHighCompression extends BaseStoredFieldsFormatTestCase {
  @Override
  protected Codec getCodec() {
    return new Lucene93Codec(Mode.BEST_COMPRESSION);
  }

  /**
   * Change compression params (leaving it the same for old segments) and tests that nothing breaks.
   */
  public void testMixedCompressions() throws Exception {
    Directory dir = newDirectory();
    for (int i = 0; i < 10; i++) {
      IndexWriterConfig iwc = newIndexWriterConfig();
      iwc.setCodec(new Lucene93Codec(RandomPicks.randomFrom(random(), Mode.values())));
      IndexWriter iw = new IndexWriter(dir, newIndexWriterConfig());
      Document doc = new Document();
      doc.add(new StoredField("field1", "value1"));
      doc.add(new StoredField("field2", "value2"));
      iw.addDocument(doc);
      if (random().nextInt(4) == 0) {
        iw.forceMerge(1);
      }
      iw.commit();
      iw.close();
    }

    DirectoryReader ir = DirectoryReader.open(dir);
    assertEquals(10, ir.numDocs());
    for (int i = 0; i < 10; i++) {
      Document doc = ir.document(i);
      assertEquals("value1", doc.get("field1"));
      assertEquals("value2", doc.get("field2"));
    }
    ir.close();
    // checkindex
    dir.close();
  }

  public void testBestSpeedCompressions() throws Exception {
    Directory dir = newDirectory();
    IndexWriterConfig iwc = newIndexWriterConfig();
    iwc.setCodec(new Lucene93Codec(Mode.BEST_SPEED));
    IndexWriter iw = new IndexWriter(dir, iwc);

    StringBuilder longValueBuilder = new StringBuilder();
    String shortValue = "value";
    longValueBuilder.append(shortValue.repeat(20 * 1024));
    String longValue = longValueBuilder.toString();

    for (int i = 0; i < 1; i++) {
      Document doc = new Document();
      doc.add(new StoredField("field1", "value1"));
      doc.add(new StoredField("field2", longValue));
      doc.add(new StoredField("field3", "value3"));
      iw.addDocument(doc);
    }

    iw.commit();
    iw.close();

    DirectoryReader ir = DirectoryReader.open(dir);
    assertEquals(1, ir.numDocs());
    Set<String> fields = new HashSet<>();
    fields.add("field1");
    for (int i = 0; i < 1; i++) {
      Document doc = ir.document(i, fields);
      assertEquals("value1", doc.get("field1"));
      assertEquals(null, doc.get("field2"));
      assertEquals(null, doc.get("field3"));
    }

    fields.add("field2");
    for (int i = 0; i < 1; i++) {
      Document doc = ir.document(i, fields);
      assertEquals("value1", doc.get("field1"));
      assertEquals(longValue, doc.get("field2"));
      assertEquals(null, doc.get("field3"));
    }

    fields = new HashSet<>();
    fields.add("field1");
    fields.add("field3");
    for (int i = 0; i < 1; i++) {
      Document doc = ir.document(i, fields);
      assertEquals("value1", doc.get("field1"));
      assertEquals(null, doc.get("field2"));
      assertEquals("value3", doc.get("field3"));
    }
    ir.close();
    // checkindex
    dir.close();
  }

  public void testInvalidOptions() {
    expectThrows(
        NullPointerException.class,
        () -> {
          new Lucene93Codec(null);
        });

    expectThrows(
        NullPointerException.class,
        () -> {
          new Lucene90StoredFieldsFormat(null);
        });
  }

  public void testShowJDKBugStatus() {
    System.err.println("JDK is buggy (JDK-8252739): " + BugfixDeflater_JDK8252739.IS_BUGGY_JDK);
  }
}
