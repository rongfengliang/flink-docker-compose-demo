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

package com.dalong.app;

import org.apache.flink.api.java.ExecutionEnvironment;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.FileSystem.WriteMode;
import com.dalong.app.util.WordCountData;
import org.apache.flink.util.Collector;
public class BatchJob {
	public static class Word {

		// fields
		private String word;
		private int frequency;

		// constructors
		public Word() {}

		public Word(String word, int i) {
			this.word = word;
			this.frequency = i;
		}

		// getters setters
		public String getWord() {
			return word;
		}

		public void setWord(String word) {
			this.word = word;
		}

		public int getFrequency() {
			return frequency;
		}

		public void setFrequency(int frequency) {
			this.frequency = frequency;
		}

		@Override
		public String toString() {
			return "Word=" + word + " freq=" + frequency;
		}
	}

	public static void main(String[] args) throws Exception {
		final ParameterTool params = ParameterTool.fromArgs(args);

		// set up the execution environment
		final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

		// make parameters available in the web interface
		env.getConfig().setGlobalJobParameters(params);

		// get input data
		DataSet<String> text;
		if (params.has("input")) {
			// read the text file from given input path
			text = env.readTextFile(params.get("input"));
		} else {
			// get default test text data
			System.out.println("Executing WordCount example with default input data set.");
			System.out.println("Use --input to specify file input.");
			text = WordCountData.getDefaultTextLineDataSet(env);
		}

		DataSet<Word> counts =
				// split up the lines into Word objects (with frequency = 1)
				text.flatMap(new Tokenizer())
						// group by the field word and sum up the frequency
						.groupBy("word")
						.reduce(new ReduceFunction<Word>() {
							@Override
							public Word reduce(Word value1, Word value2) throws Exception {
								return new Word(value1.word, value1.frequency + value2.frequency);
							}
						});

		if (params.has("output")) {
			counts.writeAsText(params.get("output"), WriteMode.OVERWRITE);
			// execute program
			env.execute("WordCount-Pojo Example");
		} else {
			System.out.println("Printing result to stdout. Use --output to specify output path.");
			counts.print();
		}

	}

	public static final class Tokenizer implements FlatMapFunction<String, Word> {

		@Override
		public void flatMap(String value, Collector<Word> out) {
			// normalize and split the line
			String[] tokens = value.toLowerCase().split("\\W+");

			// emit the pairs
			for (String token : tokens) {
				if (token.length() > 0) {
					out.collect(new Word(token, 1));
				}
			}
		}
	}

}
