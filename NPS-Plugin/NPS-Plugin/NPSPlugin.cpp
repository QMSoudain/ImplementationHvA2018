// NPSPlugin.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include "WinHttpClient.h"
#include <Authif.h>
#include <iostream>
#include <fstream>
#include <chrono>
#include <thread>
#include <Windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <ctime>
#include <iomanip>
#include <string>
#define _CRT_SECURE_NO_WARNINGS

#ifdef __cplusplus
extern "C" {
#endif

	__declspec(dllexport) DWORD WINAPI RadiusExtensionProcess2(
		__inout PRADIUS_EXTENSION_CONTROL_BLOCK pECB);

#ifdef __cplusplus
}
#endif

FILE *filepoint;
errno_t err;

void logger(const char * logmsg)
{
	time_t rawtime;
	struct tm * timeinfo;

	time(&rawtime);
	timeinfo = localtime(&rawtime);

	fopen_s(&filepoint, "C:\\DLL\\log.txt", "a");
	fprintf(filepoint, "%s", "\n");
	fprintf(filepoint, "%s", asctime(timeinfo));
	fprintf(filepoint, "%s", logmsg);
	fclose(filepoint);
}



DWORD WINAPI RadiusExtensionProcess2(
	_Inout_  PRADIUS_EXTENSION_CONTROL_BLOCK pECB)
{

	if (pECB != NULL) {
		logger("pECB is not empty");
		if (pECB->rcRequestType == rcAccessRequest)
		{
			PRADIUS_ATTRIBUTE_ARRAY pAttrs;

			pAttrs = pECB->GetRequest(pECB);
			if (pAttrs != NULL) {
				logger("pAttrs is not empty");

				//Initialize variables for the information that we are going to extract
				char * username = "";
				char * uniqueId = "";
				char * clientIp = "";

				//Size of ARRAY for loop
				DWORD size = pAttrs->GetSize(pAttrs); 

				//Array that contains all radius information
				const RADIUS_ATTRIBUTE * radiusArray;

				//Loop through array (to be able to fetch information about RADIUS request)
				for (DWORD AR = 0; AR < size; AR++) {
					//Get the attribute from the array
					radiusArray = pAttrs->AttributeAt(pAttrs, AR);
					if (radiusArray != NULL) {
						if (radiusArray->dwAttrType == ratUserName)
						{
							username = (char *)radiusArray->lpValue;
							logger(username);
						}
						if (radiusArray->dwAttrType == ratUniqueId)
						{
							uniqueId = (char *)radiusArray->dwValue;
						}
						if (radiusArray->dwAttrType == ratCallingStationId)
						{
							clientIp = (char *)radiusArray->dwValue;
						}
					}
				}

				//POST TO SERVLET
				WinHttpClient client(L"http://127.0.0.1:8080/NPS/nps");

				// Set post data.
			    string data = std::string("{\"function\":\"radiusplugin\",\"user\":\"") + username + "\",\"uniqueid\":\"test\",\"clientip\":\"test\"}";
				client.SetAdditionalDataToSend((BYTE *)data.c_str(), data.size());

				// Set request headers and timeout
				wchar_t szSize[50] = L"";
				swprintf_s(szSize, L"%d", data.size());
				wstring headers = L"Content-Length: ";
				headers += szSize;
				headers += L"\r\nContent-Type: application/json\r\n";
				client.SetAdditionalRequestHeaders(headers);

				client.SetTimeouts(30000U, 30000U, 30000U, 30000U);

				// Send HTTP post request.
				client.SendHttpRequest(L"POST");

				// Sleep for few seonds
				std::this_thread::sleep_for(std::chrono::milliseconds(3000));

				//Get Response
				wstring httpResponseHeader = client.GetResponseHeader();
				wstring httpResponseContent = client.GetResponseContent();
				
				// Process response
				if (httpResponseContent.find(L"accept") != std::string::npos) {
					logger("response is accept, granting access");
					pECB->SetResponseType(pECB, rcAccessAccept);
					logger("Access is granted");
					return NO_ERROR;
				}
				else if (httpResponseContent.find(L"reject") != std::string::npos) {
					logger("response is reject, rejecting access");
					pECB->SetResponseType(pECB, rcAccessReject);
					logger("Access is rejected");
					return NO_ERROR;
				}
				else {
					logger("response is unknown or no response was received, rejecting access");
					pECB->SetResponseType(pECB, rcAccessReject);
					logger("Access is rejected");
					return NO_ERROR;
				}
				return NO_ERROR;
			}
			return NO_ERROR;
		}
		return NO_ERROR;
	}
}
