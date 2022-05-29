package com.onectrm.akshare.api.generator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootApplication
public class AkshareJavaApiGeneratorApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(AkshareJavaApiGeneratorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Options options = new Options();
		options.addOption("docs", true, "akshare documents root folder");
		options.addOption("output", false, "output root folder");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		var files = Files.find(
						Path.of(cmd.getOptionValue("docs")),
						Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().endsWith(".md"))
				.collect(Collectors.toList());

		List<AKShareRawAPI> apis = new LinkedList<>();

		for (var file : files) {
			var lst = parseAKShareRawAPI(file);
			apis.addAll(lst);
		}


		return;

	}

	private List<AKShareRawAPI> parseAKShareRawAPI(Path filePath) throws IOException {
		var iterator = Files.lines(filePath).iterator();

		var category = StringUtils.removeEnd(filePath.getFileName().toString(), ".md");
		List<AKShareRawAPI> apis = new ArrayList<>();

		while (iterator.hasNext()) {
			var interfaceName = parse(iterator, "接口:");

			if (StringUtils.isBlank(interfaceName)) {
				break;
			}

			var source = parse(iterator, "目标地址:");
			var desc = parse(iterator, "描述:");
			var retDesc = parse(iterator, "限量:");

			var inputs = parseParameters(iterator, "输入参数");
			var output = parseParameters(iterator, "输出参数");

			AKShareRawAPI api = new AKShareRawAPI();
			api.setCategory(category);
			api.setInterfaceName(interfaceName);
			api.setSource(source);
			api.setDescription(desc);
			api.setReturnDescription(retDesc);

			api.setInput(inputs);
			api.setOutput(output);
			apis.add(api);
		}

		return apis;

	}

	private String parse(Iterator<String> iterator, String token) {
		String val = null;
		while (iterator.hasNext()) {
			var str = iterator.next();
			if (StringUtils.startsWith(str, token)) {
				var strs = StringUtils.split(str, ":", 2);
				if (strs.length == 2) {
					val = StringUtils.trimToNull(strs[1]);
				}

				break;
			}
		}

		return val;
	}

	private List<AKShareRawAPIParameter> parseParameters(Iterator<String> iterator, String token) {
		List<AKShareRawAPIParameter> parameters = null;
		while (iterator.hasNext()) {
			var str = iterator.next();
			if (StringUtils.startsWith(str, token)) {
				parameters = parseParameterTable(iterator);
				break;
			}
		}

		return parameters;
	}

	private List<AKShareRawAPIParameter> parseParameterTable(Iterator<String> iterator) {
		List<AKShareRawAPIParameter> parameters = new ArrayList<>();

		boolean flag = true;

		while (flag && iterator.hasNext()) {
			var line = iterator.next();
			line = StringUtils.replace(line, " ", "");

			if (StringUtils.startsWith(line, "|--")) {
				while (iterator.hasNext()) {
					var tableLine = iterator.next();
					var parameter = parseParameterTableRow(tableLine);
					if (parameter == null) {
						flag = false;
						break;
					}

					parameters.add(parameter);
				}
			}
		}

		return parameters.size() == 0 ? null : parameters;
	}

	private AKShareRawAPIParameter parseParameterTableRow(String str) {

		if (StringUtils.isBlank(str)) {
			return null;
		}

		var strs = StringUtils.split(str, "|");

		if (Arrays.stream(strs).allMatch(s -> StringUtils.isBlank(s))) {
			return null;
		}

		Function<String, String> extract = (val) -> {
			var s = StringUtils.trim(val);
			if (StringUtils.isBlank(s) || s.equalsIgnoreCase("-")) {
				s = null;
			}

			return s;
		};

		AKShareRawAPIParameter parameter = null;

		if (strs.length >= 3) {
			var name = extract.apply(strs[0]);
			var type = extract.apply(strs[1]);
			var desc = strs.length == 3 ? extract.apply(strs[2]) : extract.apply(strs[3]);
			var required = strs.length == 3 ? null : extract.apply(strs[2]);
			parameter = new AKShareRawAPIParameter();
			parameter.setName(name);
			parameter.setType(type);
			parameter.setRequired(required);
			parameter.setDescription(desc);
		}

		return parameter;
	}


}
