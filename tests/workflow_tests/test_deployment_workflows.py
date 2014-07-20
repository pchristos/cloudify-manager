########
# Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
#    * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    * See the License for the specific language governing permissions and
#    * limitations under the License.

__author__ = 'ran'


import uuid
from testenv import (TestCase,
                     wait_for_execution_to_end,
                     do_retries,
                     verify_workers_installation_complete,
                     send_task,
                     get_resource as resource,
                     deploy_application as deploy)


class TestDeploymentWorkflows(TestCase):

    def test_deployment_workflows(self):
        dsl_path = resource("dsl/custom_workflow_mapping.yaml")
        deployment, _ = deploy(dsl_path)
        deployment_id = deployment.id
        workflows = self.client.deployments.get(deployment_id).workflows
        self.assertEqual(3, len(workflows))
        self.assertEqual('uninstall', workflows[0].name)
        self.assertEqual('install', workflows[1].name)
        self.assertEqual('custom', workflows[2].name)

    def test_workflow_parameters_pass_from_blueprint(self):
        dsl_path = resource('dsl/workflow_parameters.yaml')
        _id = uuid.uuid1()
        blueprint_id = 'blueprint_{0}'.format(_id)
        deployment_id = 'deployment_{0}'.format(_id)
        self.client.blueprints.upload(dsl_path, blueprint_id)
        self.client.deployments.create(blueprint_id, deployment_id)
        do_retries(verify_workers_installation_complete, 30,
                   deployment_id=deployment_id)
        execution = self.client.deployments.execute(deployment_id,
                                                    'execute_operation')
        wait_for_execution_to_end(execution)

        from plugins.testmockoperations.tasks import \
            get_mock_operation_invocations

        invocations = send_task(get_mock_operation_invocations).get(timeout=10)
        self.assertEqual(1, len(invocations))
        self.assertDictEqual(invocations[0], {'test_key': 'test_value'})

    def test_get_workflow_parameters(self):
        dsl_path = resource('dsl/workflow_parameters.yaml')
        _id = uuid.uuid1()
        blueprint_id = 'blueprint_{0}'.format(_id)
        deployment_id = 'deployment_{0}'.format(_id)
        self.client.blueprints.upload(dsl_path, blueprint_id)
        self.client.deployments.create(blueprint_id, deployment_id)

        workflows = self.client.deployments.get(deployment_id).workflows
        execute_op_workflow = next(wf for wf in workflows if
                                   wf.name == 'another_execute_operation')
        expected_params = [
            {u'node_id': u'test_node'},
            u'operation',
            {
                u'properties': {
                    u'key': u'test_key',
                    u'value': u'test_value'
                }
            }
        ]
        self.assertEqual(expected_params, execute_op_workflow.parameters)