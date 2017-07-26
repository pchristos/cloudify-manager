#########
# Copyright (c) 2017 GigaSpaces Technologies Ltd. All rights reserved
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
#  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  * See the License for the specific language governing permissions and
#  * limitations under the License.
#
import requests

from flask import request

from manager_rest.storage import models
from manager_rest.storage import get_storage_manager

from manager_rest.security import SecuredResource

from manager_rest.flask_utils import get_insights_report_url

from manager_rest.manager_exceptions import NotFoundError
from manager_rest.manager_exceptions import ManagerException
from manager_rest.manager_exceptions import UnauthorizedError
from manager_rest.manager_exceptions import BadParametersError

from manager_rest.rest.rest_decorators import exceptions_handled


class InsightsReport(SecuredResource):

    @exceptions_handled
    def get(self):
        """
        Get the Insights report
        """
        token = get_storage_manager().get(models.Secret, 'insights_token')
        report = requests.get(get_insights_report_url(),
                              params=request.args,
                              headers={'Authorization': token.value})
        if not report.ok:
            if report.status_code == 400:
                raise BadParametersError()
            if report.status_code == 401:
                raise UnauthorizedError()
            if report.status_code == 404:
                raise NotFoundError()
            raise ManagerException(report.status_code, 'insights_report_error')
        return report.json()
