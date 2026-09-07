package com.narayani.bridgeline.Setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NodeSetupScreen(
    onRoleSelected: (NodeRole) -> Unit
) {

    var showMasterLogin by remember {
        mutableStateOf(false)
    }

    var authorizationId by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "BRIDGELINE",
            fontSize = 32.sp
        )

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        if (!showMasterLogin) {

            Text(
                text = "SELECT NODE TYPE",
                fontSize = 20.sp
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            // RELAY MODE
            Button(
                onClick = {
                    onRoleSelected(NodeRole.RELAY)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "RELAY MODE"
                )
            }

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            // MASTER NODE
            Button(
                onClick = {
                    showMasterLogin = true
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "MASTER NODE"
                )
            }

        } else {

            Text(
                text = "MASTER NODE",
                fontSize = 24.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = "Enter Authorization ID"
            )

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            OutlinedTextField(
                value = authorizationId,
                onValueChange = {
                    authorizationId = it
                    errorMessage = ""
                },
                label = {
                    Text("Authorization ID")
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            if (errorMessage.isNotEmpty()) {

                Text(
                    text = errorMessage
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }

            // VERIFY BUTTON
            Button(
                onClick = {

                    // TEMPORARY authorization ID
                    // Replace YOUR_NAME with your actual name
                    if (authorizationId == "CHINTHA CHAITRA") {

                        onRoleSelected(NodeRole.MASTER)

                    } else {

                        errorMessage = "Wrong Authorization ID"

                        showMasterLogin = false
                        authorizationId = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "VERIFY"
                )
            }

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            // BACK BUTTON
            Button(
                onClick = {

                    showMasterLogin = false
                    authorizationId = ""
                    errorMessage = ""

                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "BACK"
                )
            }
        }
    }
}